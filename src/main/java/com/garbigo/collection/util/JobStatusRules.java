package com.garbigo.collection.util;

import com.garbigo.collection.exception.CustomException;
import com.garbigo.collection.model.CollectionStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The moves a collector may make through PUT /status. Before this existed the
 * endpoint accepted any status at all: a collector could mark a job COMPLETED
 * without starting it (sending the completion email and unlocking rating), put
 * it back to PENDING while still assigned, or CANCEL it around the client's
 * cancellation rules. Accepting, declining, confirming and disputing have their
 * own endpoints; admins override through the admin endpoints, which have their
 * own invariants.
 *
 * ASSIGNED can go straight to IN_PROGRESS, which counts as accepting - so a
 * client that only ever calls /status keeps working.
 */
public final class JobStatusRules {

    /** Still in play: not yet finished one way or the other. */
    public static final List<CollectionStatus> ACTIVE = List.of(
            CollectionStatus.PENDING, CollectionStatus.ASSIGNED, CollectionStatus.ACCEPTED, CollectionStatus.IN_PROGRESS);

    /** Finished: completed, cancelled or disputed. */
    public static final List<CollectionStatus> FINISHED = List.of(
            CollectionStatus.COMPLETED, CollectionStatus.CANCELLED, CollectionStatus.DISPUTED);

    private static final Map<CollectionStatus, Set<CollectionStatus>> COLLECTOR_MOVES = Map.of(
            CollectionStatus.ASSIGNED, Set.of(CollectionStatus.IN_PROGRESS),
            CollectionStatus.ACCEPTED, Set.of(CollectionStatus.IN_PROGRESS),
            CollectionStatus.IN_PROGRESS, Set.of(CollectionStatus.COMPLETED));

    private JobStatusRules() {
    }

    public static void requireCollectorMove(CollectionStatus from, CollectionStatus to) {
        Set<CollectionStatus> allowed = COLLECTOR_MOVES.getOrDefault(from, Set.of());
        if (allowed.contains(to)) {
            return;
        }
        if (allowed.isEmpty()) {
            throw new CustomException("The collector can't change the status of a request that's " + from);
        }
        throw new CustomException("Cannot change status from " + from + " to " + to + ". Allowed: "
                + allowed.stream().map(Enum::name).sorted().collect(Collectors.joining(", ")));
    }
}