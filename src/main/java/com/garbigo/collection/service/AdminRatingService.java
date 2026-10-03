package com.garbigo.collection.service;

import com.garbigo.collection.dto.AdminRatingResponse;
import com.garbigo.collection.exception.NotFoundException;
import com.garbigo.collection.model.Rating;
import com.garbigo.collection.model.RequestType;
import com.garbigo.collection.repository.RatingRepository;
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
import java.util.stream.Collectors;

/** Rating moderation. Ratings can't be edited - only listed and removed (e.g. abusive comments). */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminRatingService {

    private final MongoTemplate mongoTemplate;
    private final RatingRepository ratingRepository;

    /** maxScore keeps only ratings at or below it - the quick way to find the low ones worth a look. */
    public Page<AdminRatingResponse> search(String collectorId, Integer maxScore, Pageable pageable) {
        Query query = new Query();
        if (StringUtils.hasText(collectorId)) {
            query.addCriteria(Criteria.where("collectorId").is(collectorId.trim()));
        }
        if (maxScore != null) {
            query.addCriteria(Criteria.where("score").lte(maxScore));
        }
        long total = mongoTemplate.count(query, Rating.class);
        List<AdminRatingResponse> content = mongoTemplate.find(query.with(pageable), Rating.class).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, total);
    }

    /** The request becomes rateable again afterwards, since a request can only have one rating. */
    public void delete(String adminId, String id) {
        Rating existing = ratingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rating not found: " + id));
        ratingRepository.delete(existing);
        log.info("Admin {} deleted rating {} (collector {}, score {}, request {})",
                adminId, id, existing.getCollectorId(), existing.getScore(), existing.getCollectionRequestId());
    }

    private AdminRatingResponse toResponse(Rating entity) {
        return AdminRatingResponse.builder()
                .id(entity.getId())
                .collectionRequestId(entity.getCollectionRequestId())
                .requestType(entity.getRequestType() == null ? RequestType.COLLECTION : entity.getRequestType())
                .reviewerId(entity.getReviewerId())
                .collectorId(entity.getCollectorId())
                .score(entity.getScore())
                .comment(entity.getComment())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}