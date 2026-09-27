package com.garbigo.collection.service;

import com.garbigo.collection.dto.AdminStatsResponse;
import com.garbigo.collection.model.CollectionStatus;
import com.garbigo.collection.model.ComplaintStatus;
import com.garbigo.collection.repository.CollectionRequestRepository;
import com.garbigo.collection.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final CollectionRequestRepository collectionRequestRepository;
    private final ComplaintRepository complaintRepository;
    private final UserSummaryService userSummaryService;

    public AdminStatsResponse getStats() {
        return AdminStatsResponse.builder()
                .collectionsPending(collectionRequestRepository.countByStatus(CollectionStatus.PENDING))
                .collectionsAssigned(collectionRequestRepository.countByStatus(CollectionStatus.ASSIGNED))
                .collectionsCompleted(collectionRequestRepository.countByStatus(CollectionStatus.COMPLETED))
                .collectionsCancelled(collectionRequestRepository.countByStatus(CollectionStatus.CANCELLED))
                .complaintsOpen(complaintRepository.countByStatus(ComplaintStatus.OPEN))
                .complaintsResolved(complaintRepository.countByStatus(ComplaintStatus.RESOLVED))
                .registeredCollectorsCached(userSummaryService.countCachedCollectors())
                .build();
    }
}