package com.garbigo.collection.service;

import com.garbigo.collection.dto.AdminUserDetailResponse;
import com.garbigo.collection.dto.AdminUserResponse;
import com.garbigo.collection.exception.NotFoundException;
import com.garbigo.collection.model.CollectionRequest;
import com.garbigo.collection.model.CollectionStatus;
import com.garbigo.collection.model.Complaint;
import com.garbigo.collection.model.Rating;
import com.garbigo.collection.model.SewageRequest;
import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.repository.RatingRepository;
import com.garbigo.collection.repository.UserSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Read-only view of users as this service's local cache knows them. auth-service
 * owns the real user records - creating, editing, deactivating or changing a
 * role happens there, not here - so there is deliberately no write side. If a
 * user looks missing or stale, POST /api/v1/users/resync refreshes the cache.
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final MongoTemplate mongoTemplate;
    private final UserSummaryRepository userSummaryRepository;
    private final RatingRepository ratingRepository;

    public Page<AdminUserResponse> search(String role, Boolean active, Boolean archived, String q, Pageable pageable) {
        Query query = new Query();
        if (StringUtils.hasText(role)) {
            // Stored casing isn't guaranteed (the cache counts roles case-insensitively too).
            query.addCriteria(Criteria.where("role").regex("^" + Pattern.quote(role.trim()) + "$", "i"));
        }
        if (active != null) {
            query.addCriteria(Criteria.where("active").is(active));
        }
        if (archived != null) {
            query.addCriteria(Criteria.where("archived").is(archived));
        }
        if (StringUtils.hasText(q)) {
            String contains = Pattern.quote(q.trim());
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("email").regex(contains, "i"),
                    Criteria.where("username").regex(contains, "i"),
                    Criteria.where("firstName").regex(contains, "i"),
                    Criteria.where("middleName").regex(contains, "i"),
                    Criteria.where("lastName").regex(contains, "i")));
        }

        long total = mongoTemplate.count(query, UserSummary.class);
        List<AdminUserResponse> content = mongoTemplate.find(query.with(pageable), UserSummary.class).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, total);
    }

    public AdminUserDetailResponse get(String id) {
        UserSummary user = userSummaryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "User not found in the local directory: " + id + " (it may not have synced yet - try POST /api/v1/users/resync)"));

        List<Rating> ratings = ratingRepository.findByCollectorIdIn(List.of(id));
        Double averageRating = ratings.isEmpty()
                ? null
                : ratings.stream().mapToInt(Rating::getScore).average().orElse(0);

        return AdminUserDetailResponse.builder()
                .user(toResponse(user))
                .collectionRequestsAsClient(count(CollectionRequest.class, Criteria.where("clientId").is(id)))
                .sewageRequestsAsClient(count(SewageRequest.class, Criteria.where("clientId").is(id)))
                .collectionJobsAsCollector(count(CollectionRequest.class, Criteria.where("collectorId").is(id)))
                .sewageJobsAsCollector(count(SewageRequest.class, Criteria.where("collectorId").is(id)))
                .completedJobsAsCollector(
                        count(CollectionRequest.class, Criteria.where("collectorId").is(id).and("status").is(CollectionStatus.COMPLETED))
                                + count(SewageRequest.class, Criteria.where("collectorId").is(id).and("status").is(CollectionStatus.COMPLETED)))
                .complaintsFiled(count(Complaint.class, Criteria.where("reporterId").is(id)))
                .averageRating(averageRating)
                .ratingCount(ratings.size())
                .build();
    }

    private long count(Class<?> type, Criteria criteria) {
        return mongoTemplate.count(new Query(criteria), type);
    }

    private AdminUserResponse toResponse(UserSummary user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.preferredName())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.isActive())
                .archived(user.isArchived())
                .build();
    }
}