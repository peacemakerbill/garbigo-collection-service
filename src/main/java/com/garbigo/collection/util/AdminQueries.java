package com.garbigo.collection.util;

import com.garbigo.collection.dto.AdminRequestFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.StringUtils;

import java.time.Instant;

/** Query building shared by the admin listings, so collections and sewage filter identically. */
public final class AdminQueries {

    private AdminQueries() {
    }

    /** Builds the filter query for either request type; wasteType applies to collections, urgency to sewage. */
    public static Query requestQuery(AdminRequestFilter filter, boolean sewage) {
        Query query = new Query();
        if (filter.getStatus() != null) {
            query.addCriteria(Criteria.where("status").is(filter.getStatus()));
        }
        if (filter.getPaymentStatus() != null) {
            query.addCriteria(Criteria.where("paymentStatus").is(filter.getPaymentStatus()));
        }
        if (StringUtils.hasText(filter.getClientId())) {
            query.addCriteria(Criteria.where("clientId").is(filter.getClientId().trim()));
        }
        if (StringUtils.hasText(filter.getCollectorId())) {
            query.addCriteria(Criteria.where("collectorId").is(filter.getCollectorId().trim()));
        }
        if (sewage) {
            if (filter.getUrgency() != null) {
                query.addCriteria(Criteria.where("urgency").is(filter.getUrgency()));
            }
        } else if (filter.getWasteType() != null) {
            query.addCriteria(Criteria.where("wasteType").is(filter.getWasteType()));
        }
        Criteria created = createdBetween(filter.getCreatedFrom(), filter.getCreatedTo());
        if (created != null) {
            query.addCriteria(created);
        }
        return query;
    }

    /** createdAt between the two bounds (both inclusive, either optional); null if neither is given. */
    public static Criteria createdBetween(Instant from, Instant to) {
        if (from == null && to == null) {
            return null;
        }
        Criteria criteria = Criteria.where("createdAt");
        if (from != null) {
            criteria = criteria.gte(from);
        }
        if (to != null) {
            criteria = criteria.lte(to);
        }
        return criteria;
    }
}