package com.garbigo.collection.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserSummaryService userSummaryService;
    private final MailService mailService;

    public ComplaintResponse create(String reporterId, ComplaintCreateRequest request) {
        Complaint saved = complaintRepository.save(
                Complaint.builder()
                        .collectionRequestId(request.getCollectionRequestId())
                        .reporterId(reporterId)
                        .category(request.getCategory())
                        .description(request.getDescription())
                        .status(ComplaintStatus.OPEN)
                        .build()
        );
        return toResponse(saved);
    }

    public Page<ComplaintResponse> getMine(String reporterId, Pageable pageable) {
        return complaintRepository.findByReporterId(reporterId, pageable)
                .map(this::toResponse);
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
                        reporter.preferredName(),
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