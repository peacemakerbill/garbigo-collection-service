package com.garbigo.collection.service;

import com.garbigo.collection.dto.ComplaintCreateRequest;
import com.garbigo.collection.dto.ComplaintResponse;
import com.garbigo.collection.exception.CustomException;
import com.garbigo.collection.model.Complaint;
import com.garbigo.collection.model.ComplaintStatus;
import com.garbigo.collection.notification.MailService;
import com.garbigo.collection.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for {@link Complaint}.
 *
 * <p>TODO: scaffolding-level implementation - nothing here yet confirms
 * collectionRequestId actually refers to a real CollectionRequest before
 * accepting a complaint against it.
 */
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserSummaryService userSummaryService;
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

    /**
     * Best-effort notification - a missing/stale UserSummary cache entry
     * just means no email goes out, not a failed resolve.
     */
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
        return ComplaintResponse.builder()
                .id(entity.getId())
                .collectionRequestId(entity.getCollectionRequestId())
                .reporterId(entity.getReporterId())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}