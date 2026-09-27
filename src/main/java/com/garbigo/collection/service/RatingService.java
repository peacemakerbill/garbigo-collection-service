package com.garbigo.collection.service;

import com.garbigo.collection.dto.RatingCreateRequest;
import com.garbigo.collection.dto.RatingResponse;
import com.garbigo.collection.exception.CustomException;
import com.garbigo.collection.model.CollectionRequest;
import com.garbigo.collection.model.CollectionStatus;
import com.garbigo.collection.model.Rating;
import com.garbigo.collection.repository.CollectionRequestRepository;
import com.garbigo.collection.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final CollectionRequestRepository collectionRequestRepository;

    public RatingResponse rate(String collectionRequestId, String reviewerId, RatingCreateRequest request) {
        CollectionRequest collectionRequest = collectionRequestRepository.findById(collectionRequestId)
                .orElseThrow(() -> new CustomException("Collection request not found: " + collectionRequestId));

        if (!reviewerId.equals(collectionRequest.getClientId())) {
            throw new CustomException("Only the requesting client can rate this request");
        }
        if (collectionRequest.getStatus() != CollectionStatus.COMPLETED) {
            throw new CustomException("Can only rate a completed request");
        }
        if (ratingRepository.existsByCollectionRequestId(collectionRequestId)) {
            throw new CustomException("This request has already been rated");
        }

        Rating saved = ratingRepository.save(
                Rating.builder()
                        .collectionRequestId(collectionRequestId)
                        .reviewerId(reviewerId)
                        .collectorId(collectionRequest.getCollectorId())
                        .score(request.getScore())
                        .comment(request.getComment())
                        .build()
        );
        return toResponse(saved);
    }

    private RatingResponse toResponse(Rating entity) {
        return RatingResponse.builder()
                .id(entity.getId())
                .collectionRequestId(entity.getCollectionRequestId())
                .collectorId(entity.getCollectorId())
                .score(entity.getScore())
                .comment(entity.getComment())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}