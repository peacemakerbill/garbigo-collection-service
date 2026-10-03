package com.garbigo.collection.service;

import com.garbigo.collection.dto.AdminStatsResponse;
import com.garbigo.collection.dto.CollectorPerformanceResponse;
import com.garbigo.collection.dto.ComplaintStatsResponse;
import com.garbigo.collection.dto.DailyRequestCountResponse;
import com.garbigo.collection.dto.RevenueStatsResponse;
import com.garbigo.collection.exception.CustomException;
import com.garbigo.collection.model.CollectionRequest;
import com.garbigo.collection.model.CollectionStatus;
import com.garbigo.collection.model.Complaint;
import com.garbigo.collection.model.ComplaintCategory;
import com.garbigo.collection.model.ComplaintStatus;
import com.garbigo.collection.model.PaymentStatus;
import com.garbigo.collection.model.Rating;
import com.garbigo.collection.model.RecurringSchedule;
import com.garbigo.collection.model.SewageRequest;
import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.repository.RatingRepository;
import com.garbigo.collection.util.AdminQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Admin statistics. Everything is computed from reads of only the fields it
 * needs, summed or grouped in Java. That's deliberate for the money figures:
 * Spring Data stores BigDecimal as a string by default, so a Mongo-side $sum on
 * quotedPrice would silently add up to zero. It's also fine at this scale, but
 * these read every matching document, so on a very large dataset the per-day,
 * revenue and collector figures are the ones to move to real aggregations.
 */
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private static final int STALE_COMPLAINT_DAYS = 7;
    private static final String UNSPECIFIED_CURRENCY = "UNSPECIFIED";

    private final MongoTemplate mongoTemplate;
    private final RatingRepository ratingRepository;
    private final UserSummaryService userSummaryService;

    // ------------------------------------------------------------------
    // Overview
    // ------------------------------------------------------------------

    public AdminStatsResponse getStats() {
        Map<String, Long> collectionsByStatus = countByJobStatus(CollectionRequest.class);
        Map<String, Long> sewageByStatus = countByJobStatus(SewageRequest.class);

        Map<String, Long> paymentStatusCounts = new LinkedHashMap<>();
        for (PaymentStatus status : PaymentStatus.values()) {
            paymentStatusCounts.put(status.name(),
                    count(CollectionRequest.class, Criteria.where("paymentStatus").is(status))
                            + count(SewageRequest.class, Criteria.where("paymentStatus").is(status)));
        }

        Query scores = new Query();
        scores.fields().include("score");
        List<Rating> ratings = mongoTemplate.find(scores, Rating.class);
        Double averageRating = ratings.isEmpty()
                ? null
                : ratings.stream().mapToInt(Rating::getScore).average().orElse(0);

        return AdminStatsResponse.builder()
                .collectionsPending(collectionsByStatus.get(CollectionStatus.PENDING.name()))
                .collectionsAssigned(collectionsByStatus.get(CollectionStatus.ASSIGNED.name()))
                .collectionsCompleted(collectionsByStatus.get(CollectionStatus.COMPLETED.name()))
                .collectionsCancelled(collectionsByStatus.get(CollectionStatus.CANCELLED.name()))
                .complaintsOpen(count(Complaint.class, Criteria.where("status").is(ComplaintStatus.OPEN)))
                .complaintsResolved(count(Complaint.class, Criteria.where("status").is(ComplaintStatus.RESOLVED)))
                .complaintsInReview(count(Complaint.class, Criteria.where("status").is(ComplaintStatus.IN_REVIEW)))
                .registeredCollectorsCached(userSummaryService.countCachedCollectors())
                .registeredClientsCached(userSummaryService.countCachedClients())
                .activeSchedules(count(RecurringSchedule.class, Criteria.where("active").is(true)))
                .totalRatings(ratings.size())
                .averageRating(averageRating)
                .collectionsByStatus(collectionsByStatus)
                .sewageByStatus(sewageByStatus)
                .paymentStatusCounts(paymentStatusCounts)
                .build();
    }

    // ------------------------------------------------------------------
    // Revenue
    // ------------------------------------------------------------------

    /**
     * Collection and sewage requests that have a price, grouped by currency.
     * from/to are inclusive calendar days (UTC) on when the request was created -
     * not when it was paid, since this service doesn't record a payment date.
     */
    public RevenueStatsResponse getRevenue(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new CustomException("'from' must not be after 'to'");
        }
        Criteria created = AdminQueries.createdBetween(startOfDay(from), endOfDay(to));

        List<PricedRequest> priced = new ArrayList<>();
        for (CollectionRequest request : mongoTemplate.find(pricedQuery(created), CollectionRequest.class)) {
            priced.add(new PricedRequest(request.getQuotedPrice(), request.getCurrency(), request.getPaymentStatus(), request.getStatus()));
        }
        for (SewageRequest request : mongoTemplate.find(pricedQuery(created), SewageRequest.class)) {
            priced.add(new PricedRequest(request.getQuotedPrice(), request.getCurrency(), request.getPaymentStatus(), request.getStatus()));
        }

        Map<String, Totals> byCurrency = new TreeMap<>();
        for (PricedRequest request : priced) {
            if (request.price() == null) {
                continue;
            }
            Totals totals = byCurrency.computeIfAbsent(
                    request.currency() == null ? UNSPECIFIED_CURRENCY : request.currency(), currency -> new Totals());
            PaymentStatus payment = request.paymentStatus() == null ? PaymentStatus.UNPAID : request.paymentStatus();
            switch (payment) {
                case PAID -> {
                    totals.paid = totals.paid.add(request.price());
                    totals.paidCount++;
                }
                case REFUNDED -> {
                    totals.refunded = totals.refunded.add(request.price());
                    totals.refundedCount++;
                }
                default -> {
                    // UNPAID, PENDING, FAILED: money still expected - unless the job was cancelled.
                    if (request.status() != CollectionStatus.CANCELLED) {
                        totals.outstanding = totals.outstanding.add(request.price());
                        totals.outstandingCount++;
                    }
                }
            }
        }

        List<RevenueStatsResponse.CurrencyTotals> rows = byCurrency.entrySet().stream()
                .map(entry -> RevenueStatsResponse.CurrencyTotals.builder()
                        .currency(entry.getKey())
                        .paidTotal(entry.getValue().paid)
                        .paidCount(entry.getValue().paidCount)
                        .outstandingTotal(entry.getValue().outstanding)
                        .outstandingCount(entry.getValue().outstandingCount)
                        .refundedTotal(entry.getValue().refunded)
                        .refundedCount(entry.getValue().refundedCount)
                        .build())
                .collect(Collectors.toList());

        return RevenueStatsResponse.builder().from(from).to(to).byCurrency(rows).build();
    }

    // ------------------------------------------------------------------
    // Requests per day
    // ------------------------------------------------------------------

    /** New requests per UTC day for the last `days` days including today; days with none are included as zeros. */
    public List<DailyRequestCountResponse> getRequestsPerDay(int days) {
        if (days < 1 || days > 365) {
            throw new CustomException("days must be between 1 and 365");
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate firstDay = today.minusDays(days - 1L);
        Instant since = startOfDay(firstDay);

        Map<LocalDate, long[]> buckets = new TreeMap<>();
        for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
            buckets.put(day, new long[2]);
        }

        for (CollectionRequest request : mongoTemplate.find(createdSinceQuery(since), CollectionRequest.class)) {
            addToBucket(buckets, request.getCreatedAt(), 0);
        }
        for (SewageRequest request : mongoTemplate.find(createdSinceQuery(since), SewageRequest.class)) {
            addToBucket(buckets, request.getCreatedAt(), 1);
        }

        return buckets.entrySet().stream()
                .map(entry -> DailyRequestCountResponse.builder()
                        .date(entry.getKey())
                        .collections(entry.getValue()[0])
                        .sewage(entry.getValue()[1])
                        .total(entry.getValue()[0] + entry.getValue()[1])
                        .build())
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Collector performance
    // ------------------------------------------------------------------

    /** Collectors ranked by completed jobs, then average rating. Only collectors who have been assigned at least one job appear. */
    public List<CollectorPerformanceResponse> getCollectorPerformance(int limit) {
        if (limit < 1 || limit > 100) {
            throw new CustomException("limit must be between 1 and 100");
        }

        Map<String, Tally> tallies = new HashMap<>();
        Query collections = new Query(Criteria.where("collectorId").ne(null));
        collections.fields().include("collectorId").include("status");
        for (CollectionRequest request : mongoTemplate.find(collections, CollectionRequest.class)) {
            tally(tallies, request.getCollectorId(), request.getStatus());
        }
        Query sewage = new Query(Criteria.where("collectorId").ne(null));
        sewage.fields().include("collectorId").include("status");
        for (SewageRequest request : mongoTemplate.find(sewage, SewageRequest.class)) {
            tally(tallies, request.getCollectorId(), request.getStatus());
        }
        if (tallies.isEmpty()) {
            return List.of();
        }

        Map<String, List<Rating>> ratingsByCollector = ratingRepository.findByCollectorIdIn(new ArrayList<>(tallies.keySet())).stream()
                .collect(Collectors.groupingBy(Rating::getCollectorId));
        Map<String, UserSummary> users = userSummaryService.findAllByIds(tallies.keySet());

        List<CollectorPerformanceResponse> all = new ArrayList<>();
        for (Map.Entry<String, Tally> entry : tallies.entrySet()) {
            List<Rating> ratings = ratingsByCollector.getOrDefault(entry.getKey(), List.of());
            UserSummary user = users.get(entry.getKey());
            all.add(CollectorPerformanceResponse.builder()
                    .collectorId(entry.getKey())
                    .name(user == null ? null : user.preferredName())
                    .completedJobs(entry.getValue().completed)
                    .openJobs(entry.getValue().open)
                    .disputedJobs(entry.getValue().disputed)
                    .averageRating(ratings.isEmpty() ? null : ratings.stream().mapToInt(Rating::getScore).average().orElse(0))
                    .ratingCount(ratings.size())
                    .build());
        }

        all.sort((a, b) -> {
            int byCompleted = Long.compare(b.getCompletedJobs(), a.getCompletedJobs());
            return byCompleted != 0 ? byCompleted : Double.compare(ratingOf(b), ratingOf(a));
        });
        return all.size() > limit ? new ArrayList<>(all.subList(0, limit)) : all;
    }

    // ------------------------------------------------------------------
    // Complaints
    // ------------------------------------------------------------------

    public ComplaintStatsResponse getComplaintStats() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ComplaintStatus status : ComplaintStatus.values()) {
            byStatus.put(status.name(), count(Complaint.class, Criteria.where("status").is(status)));
        }
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (ComplaintCategory category : ComplaintCategory.values()) {
            byCategory.put(category.name(), count(Complaint.class, Criteria.where("category").is(category)));
        }

        Instant staleBefore = Instant.now().minus(STALE_COMPLAINT_DAYS, ChronoUnit.DAYS);
        long stale = count(Complaint.class,
                Criteria.where("status").ne(ComplaintStatus.RESOLVED).and("createdAt").lt(staleBefore));

        return ComplaintStatsResponse.builder()
                .total(byStatus.values().stream().mapToLong(Long::longValue).sum())
                .byStatus(byStatus)
                .byCategory(byCategory)
                .staleAfterDays(STALE_COMPLAINT_DAYS)
                .staleUnresolved(stale)
                .build();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private long count(Class<?> type, Criteria criteria) {
        return mongoTemplate.count(new Query(criteria), type);
    }

    private Map<String, Long> countByJobStatus(Class<?> type) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (CollectionStatus status : CollectionStatus.values()) {
            counts.put(status.name(), count(type, Criteria.where("status").is(status)));
        }
        return counts;
    }

    private Query pricedQuery(Criteria created) {
        Query query = new Query(Criteria.where("quotedPrice").ne(null));
        if (created != null) {
            query.addCriteria(created);
        }
        query.fields().include("quotedPrice").include("currency").include("paymentStatus").include("status");
        return query;
    }

    private Query createdSinceQuery(Instant since) {
        Query query = new Query(Criteria.where("createdAt").gte(since));
        query.fields().include("createdAt");
        return query;
    }

    private void addToBucket(Map<LocalDate, long[]> buckets, Instant createdAt, int index) {
        if (createdAt == null) {
            return;
        }
        long[] bucket = buckets.get(createdAt.atZone(ZoneOffset.UTC).toLocalDate());
        if (bucket != null) {
            bucket[index]++;
        }
    }

    private void tally(Map<String, Tally> tallies, String collectorId, CollectionStatus status) {
        Tally tally = tallies.computeIfAbsent(collectorId, id -> new Tally());
        if (status == null) {
            return;
        }
        switch (status) {
            case COMPLETED -> tally.completed++;
            case ASSIGNED, IN_PROGRESS -> tally.open++;
            case DISPUTED -> tally.disputed++;
            default -> {
                // PENDING / CANCELLED don't count toward a collector's record.
            }
        }
    }

    private double ratingOf(CollectorPerformanceResponse response) {
        return response.getAverageRating() == null ? 0.0 : response.getAverageRating();
    }

    private Instant startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /** Last millisecond of the day, so an inclusive 'to' date includes that whole day. */
    private Instant endOfDay(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1);
    }

    private record PricedRequest(BigDecimal price, String currency, PaymentStatus paymentStatus, CollectionStatus status) {
    }

    private static final class Totals {
        private BigDecimal paid = BigDecimal.ZERO;
        private BigDecimal outstanding = BigDecimal.ZERO;
        private BigDecimal refunded = BigDecimal.ZERO;
        private long paidCount;
        private long outstandingCount;
        private long refundedCount;
    }

    private static final class Tally {
        private long completed;
        private long open;
        private long disputed;
    }
}