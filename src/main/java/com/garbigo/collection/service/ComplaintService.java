package com.garbigo.collection.service;

import com.garbigo.collection.dto.ComplaintCreateRequest;
import com.garbigo.collection.dto.ComplaintResponse;
import com.garbigo.collection.exception.CustomException;
import com.garbigo.collection.exception.NotFoundException;
import com.garbigo.collection.model.Complaint;
import com.garbigo.collection.model.ComplaintCategory;
import com.garbigo.collection.model.ComplaintStatus;
import com.garbigo.collection.model.RequestType;
import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.notification.MailService;
import com.garbigo.collection.repository.CollectionRequestRepository;
import com.garbigo.collection.repository.ComplaintRepository;
import com.garbigo.collection.repository.SewageRequestRepository;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final CollectionRequestRepository collectionRequestRepository;
    private final SewageRequestRepository sewageRequestRepository;
    private final UserSummaryService userSummaryService;
    private final MailService mailService;
    private final MongoTemplate mongoTemplate;

    /**
     * The reporter has to be a party to the request they're complaining
     * about - the client who made it, or the collector it's assigned to.
     * Otherwise anyone could file complaints against any request ID, real
     * or invented. A request that doesn't exist and one the reporter has
     * no part in get the same 404, so this can't be used to probe which
     * IDs exist (same rule as getById on the requests themselves).
     */
    public ComplaintResponse create(String reporterId, ComplaintCreateRequest request) {
        RequestType requestType = partyRequestType(reporterId, request.getCollectionRequestId());

        Complaint saved = complaintRepository.save(
                Complaint.builder()
                        .collectionRequestId(request.getCollectionRequestId())
                        .requestType(requestType)
                        .reporterId(reporterId)
                        .category(request.getCategory())
                        .description(request.getDescription())
                        .status(ComplaintStatus.OPEN)
                        .build()
        );
        return toResponse(saved);
    }

    /**
     * Which kind of request the reporter is a party to. The field is still called
     * collectionRequestId, but it can now hold a sewage request's id - ids are
     * unique across both, so whichever collection has it is the answer. A request
     * that doesn't exist and one the reporter has no part in give the same 404,
     * so this can't be used to probe which ids exist.
     */
    private RequestType partyRequestType(String reporterId, String requestId) {
        boolean collectionParty = collectionRequestRepository.findById(requestId)
                .filter(target -> reporterId.equals(target.getClientId()) || reporterId.equals(target.getCollectorId()))
                .isPresent();
        if (collectionParty) {
            return RequestType.COLLECTION;
        }
        boolean sewageParty = sewageRequestRepository.findById(requestId)
                .filter(target -> reporterId.equals(target.getClientId()) || reporterId.equals(target.getCollectorId()))
                .isPresent();
        if (sewageParty) {
            return RequestType.SEWAGE;
        }
        throw new NotFoundException("Request not found: " + requestId);
    }

    public Page<ComplaintResponse> getMine(String reporterId, Pageable pageable) {
        return complaintRepository.findByReporterId(reporterId, pageable)
                .map(this::toResponse);
    }

    /**
     * Rejects an already-resolved complaint rather than quietly re-resolving
     * it - each resolve sends the reporter an email, so a repeat call
     * (double-click, retried request) would otherwise email them again.
     */
    public ComplaintResponse resolve(String id) {
        Complaint existing = complaintRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Complaint not found: " + id));
        if (existing.getStatus() == ComplaintStatus.RESOLVED) {
            throw new CustomException("This complaint is already resolved");
        }
        existing.setStatus(ComplaintStatus.RESOLVED);
        Complaint saved = complaintRepository.save(existing);
        notifyReporterOfResolution(saved);
        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Admin operations (see AdminComplaintController).
    // ------------------------------------------------------------------

    public Page<ComplaintResponse> adminSearch(
            ComplaintStatus status, ComplaintCategory category, String reporterId, String collectionRequestId, Pageable pageable) {
        Query query = new Query();
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (category != null) {
            query.addCriteria(Criteria.where("category").is(category));
        }
        if (StringUtils.hasText(reporterId)) {
            query.addCriteria(Criteria.where("reporterId").is(reporterId.trim()));
        }
        if (StringUtils.hasText(collectionRequestId)) {
            query.addCriteria(Criteria.where("collectionRequestId").is(collectionRequestId.trim()));
        }

        long total = mongoTemplate.count(query, Complaint.class);
        List<Complaint> found = mongoTemplate.find(query.with(pageable), Complaint.class);

        // Reporter names for the whole page in one query, from the cache - not a
        // resolve() per row, which could trigger a full auth-service refresh per
        // unknown reporter.
        Map<String, UserSummary> reporters = userSummaryService.findAllByIds(
                found.stream().map(Complaint::getReporterId).filter(Objects::nonNull).collect(Collectors.toSet()));
        List<ComplaintResponse> content = found.stream()
                .map(complaint -> toResponse(complaint, reporters.get(complaint.getReporterId())))
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, total);
    }

    public ComplaintResponse adminGet(String id) {
        return toResponse(findOrThrow(id));
    }

    /**
     * RESOLVED goes through resolve() so the reporter gets their email and a
     * repeat is rejected; OPEN and IN_REVIEW just move the status, which also
     * lets an admin reopen a complaint that was resolved by mistake.
     */
    public ComplaintResponse adminSetStatus(String adminId, String id, ComplaintStatus newStatus) {
        if (newStatus == ComplaintStatus.RESOLVED) {
            log.info("Admin {} is resolving complaint {}", adminId, id);
            return resolve(id);
        }
        Complaint existing = findOrThrow(id);
        if (existing.getStatus() == newStatus) {
            throw new CustomException("This complaint is already " + newStatus);
        }
        ComplaintStatus previous = existing.getStatus();
        existing.setStatus(newStatus);
        Complaint saved = complaintRepository.save(existing);
        log.info("Admin {} changed complaint {} status {} -> {}", adminId, id, previous, newStatus);
        return toResponse(saved);
    }

    private Complaint findOrThrow(String id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Complaint not found: " + id));
    }

    private void notifyReporterOfResolution(Complaint complaint) {
        userSummaryService.findById(complaint.getReporterId()).ifPresent(reporter ->
                mailService.sendComplaintResolved(
                        reporter.getEmail(),
                        reporter.preferredName(),
                        complaint.getId(),
                        complaint.getDescription()
                )
        );
    }

    private ComplaintResponse toResponse(Complaint entity) {
        return toResponse(entity, resolveReporterSummary(entity.getReporterId()));
    }

    private ComplaintResponse toResponse(Complaint entity, UserSummary reporter) {
        return ComplaintResponse.builder()
                .id(entity.getId())
                .collectionRequestId(entity.getCollectionRequestId())
                .requestType(entity.getRequestType() == null ? RequestType.COLLECTION : entity.getRequestType())
                .reporterId(entity.getReporterId())
                .reporterName(reporter == null ? null : reporter.preferredName())
                .reporterEmail(reporter == null ? null : reporter.getEmail())
                .category(entity.getCategory())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Supplementary data, not core to the complaint itself - an unreachable
     * auth-service degrades to null names here rather than failing the
     * whole request (contrast with CollectionRequestService.assign, where
     * a connectivity failure propagates as a 503 instead).
     */
    private UserSummary resolveReporterSummary(String reporterId) {
        try {
            return userSummaryService.resolve(reporterId).orElse(null);
        } catch (RetryableException e) {
            log.warn("auth-service unreachable while enriching complaint reporter {}: {}", reporterId, e.getMessage());
            return null;
        }
    }
}