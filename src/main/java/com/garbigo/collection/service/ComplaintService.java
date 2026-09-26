package com.garbigo.collection.service;

import com.garbigo.collection.client.AuthServiceClient;
import com.garbigo.collection.dto.ComplaintCreateRequest;
import com.garbigo.collection.dto.ComplaintResponse;
import com.garbigo.collection.exception.CustomException;
import com.garbigo.collection.model.Complaint;
import com.garbigo.collection.model.ComplaintStatus;
import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.notification.MailService;
import com.garbigo.collection.repository.ComplaintRepository;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserSummaryService userSummaryService;
    private final AuthServiceClient authServiceClient;
    private final MailService mailService;

    public ComplaintResponse create(String reporterId, ComplaintCreateRequest request) {
        Complaint saved = complaintRepository.save(
                Complaint.builder()
                        .collectionRequestId(request.getCollectionRequestId())
                        .reporterId(reporterId)
                        .description(request.getDescription())
                        .status(ComplaintStatus.OPEN)
                        .build()
        );
        return toResponse(saved);
    }

    public List<ComplaintResponse> getMine(String reporterId) {
        return complaintRepository.findByReporterId(reporterId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ComplaintResponse resolve(String id) {
        Complaint existing = complaintRepository.findById(id)
                .orElseThrow(() -> new CustomException("Complaint not found: " + id));
        existing.setStatus(ComplaintStatus.RESOLVED);
        Complaint saved = complaintRepository.save(existing);
        notifyReporterOfResolution(saved);
        return toResponse(saved);
    }

    private void notifyReporterOfResolution(Complaint complaint) {
        userSummaryService.findById(complaint.getReporterId()).ifPresent(reporter ->
                mailService.sendComplaintResolved(
                        reporter.getEmail(),
                        reporter.getDisplayUsername(),
                        complaint.getId(),
                        complaint.getDescription()
                )
        );
    }

    private ComplaintResponse toResponse(Complaint entity) {
        UserSummary reporter = resolveReporterSummary(entity.getReporterId());

        return ComplaintResponse.builder()
                .id(entity.getId())
                .collectionRequestId(entity.getCollectionRequestId())
                .reporterId(entity.getReporterId())
                .reporterName(reporter == null ? null : reporter.getDisplayUsername())
                .reporterEmail(reporter == null ? null : reporter.getEmail())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Cache first; on a miss, falls back to a live auth-service call and
     * caches the result. Supplementary data, not core to the complaint
     * itself, so an unreachable auth-service degrades to null names here
     * rather than failing the whole request (contrast with
     * CollectionRequestService.assign, where auth-service being reachable
     * actually matters and a failure propagates as a 503 instead).
     */
    private UserSummary resolveReporterSummary(String reporterId) {
        return userSummaryService.findById(reporterId)
                .orElseGet(() -> fetchAndCacheFromAuthService(reporterId));
    }

    private UserSummary fetchAndCacheFromAuthService(String reporterId) {
        try {
            UserSummary fetched = authServiceClient.getUserById(reporterId);
            if (fetched != null) {
                userSummaryService.upsert(fetched);
            }
            return fetched;
        } catch (RetryableException e) {
            log.warn("auth-service unreachable while enriching complaint reporter {}: {}", reporterId, e.getMessage());
            return null;
        }
    }
}