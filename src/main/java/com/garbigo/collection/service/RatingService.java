package com.garbigo.collection.service;

import com.garbigo.collection.dto.RatingCreateRequest;
import com.garbigo.collection.dto.RatingResponse;
import com.garbigo.collection.exception.CustomException;
import com.garbigo.collection.exception.NotFoundException;
import com.garbigo.collection.model.CollectionRequest;
import com.garbigo.collection.model.CollectionStatus;
import com.garbigo.collection.model.Rating;
import com.garbigo.collection.model.RequestType;
import com.garbigo.collection.model.SewageRequest;
import com.garbigo.collection.repository.CollectionRequestRepository;
import com.garbigo.collection.repository.RatingRepository;
import com.garbigo.collection.repository.SewageRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final CollectionRequestRepository collectionRequestRepository;
    private final SewageRequestRepository sewageRequestRepository;

    public RatingResponse rate(String requestId, String reviewerId, RatingCreateRequest request) {
        CollectionRequest target = collectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Collection request not found: " + requestId));
        return saveRating(RequestType.COLLECTION, requestId, reviewerId,
                target.getClientId(), target.getStatus(), target.getCollectorId(), request);
    }

    public RatingResponse rateSewage(String requestId, String reviewerId, RatingCreateRequest request) {
        SewageRequest target = sewageRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Sewage request not found: " + requestId));
        return saveRating(RequestType.SEWAGE, requestId, reviewerId,
                target.getClientId(), target.getStatus(), target.getCollectorId(), request);
    }

    /**
     * The rating's collectionRequestId field holds the id of whichever kind of
     * request it is - the name predates sewage ratings, and ids are unique across
     * both collections, so the unique index and the one-rating-per-request check
     * still hold. requestType says which kind.
     */
    private RatingResponse saveRating(
            RequestType type, String requestId, String reviewerId,
            String clientId, CollectionStatus status, String collectorId, RatingCreateRequest request) {
        if (!reviewerId.equals(clientId)) {
            throw new CustomException("Only the requesting client can rate this request");
        }
        if (status != CollectionStatus.COMPLETED) {
            throw new CustomException("Can only rate a completed request");
        }
        if (ratingRepository.existsByCollectionRequestId(requestId)) {
            throw new CustomException("This request has already been rated");
        }

        Rating saved = ratingRepository.save(
                Rating.builder()
                        .collectionRequestId(requestId)
                        .requestType(type)
                        .reviewerId(reviewerId)
                        .collectorId(collectorId)
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
                .requestType(entity.getRequestType() == null ? RequestType.COLLECTION : entity.getRequestType())
                .collectorId(entity.getCollectorId())
                .score(entity.getScore())
                .comment(entity.getComment())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}