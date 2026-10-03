package com.garbigo.collection.util;

import com.garbigo.collection.dto.MyRequestFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/** Query building for the per-user request lists (the admin listings use AdminQueries). */
public final class RequestQueries {

    private RequestQueries() {
    }

    /** Requests where `ownerField` ("clientId" or "collectorId") is the user, narrowed by the optional filter. */
    public static Query ownedBy(String ownerField, String userId, MyRequestFilter filter) {
        Query query = new Query(Criteria.where(ownerField).is(userId));
        if (filter == null) {
            return query;
        }
        // One criterion on "status" only - Mongo can't take two for the same key.
        if (filter.getStatus() != null) {
            query.addCriteria(Criteria.where("status").is(filter.getStatus()));
        } else if (filter.getActive() != null) {
            query.addCriteria(Criteria.where("status").in(
                    filter.getActive() ? JobStatusRules.ACTIVE : JobStatusRules.FINISHED));
        }
        if (filter.getScheduledFrom() != null || filter.getScheduledTo() != null) {
            Criteria scheduled = Criteria.where("scheduledAt");
            if (filter.getScheduledFrom() != null) {
                scheduled = scheduled.gte(filter.getScheduledFrom());
            }
            if (filter.getScheduledTo() != null) {
                scheduled = scheduled.lte(filter.getScheduledTo());
            }
            query.addCriteria(scheduled);
        }
        return query;
    }
}