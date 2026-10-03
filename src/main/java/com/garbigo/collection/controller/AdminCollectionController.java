package com.garbigo.collection.controller;

import com.garbigo.collection.dto.AdminRequestFilter;
import com.garbigo.collection.dto.CollectionRequestCreateRequest;
import com.garbigo.collection.dto.CollectionRequestResponse;
import com.garbigo.collection.model.CollectionStatus;
import com.garbigo.collection.model.PaymentStatus;
import com.garbigo.collection.model.WasteType;
import com.garbigo.collection.security.CallerRoles;
import com.garbigo.collection.service.CollectionRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Admin management of collection requests. There is deliberately no hard delete:
 * a request can carry a price, a payment, ratings and complaints, so the
 * admin way to remove one from play is PUT /{id}/status?status=CANCELLED.
 */
@RestController
@RequestMapping("/api/v1/admin/collections")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCollectionController {

    private final CollectionRequestService collectionRequestService;

    @GetMapping
    public ResponseEntity<Page<CollectionRequestResponse>> search(
            @RequestParam(required = false) CollectionStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String collectorId,
            @RequestParam(required = false) WasteType wasteType,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        AdminRequestFilter filter = AdminRequestFilter.builder()
                .status(status).paymentStatus(paymentStatus).clientId(clientId).collectorId(collectorId)
                .wasteType(wasteType).createdFrom(createdFrom).createdTo(createdTo)
                .build();
        return ResponseEntity.ok(collectionRequestService.adminSearch(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollectionRequestResponse> get(Authentication authentication, @PathVariable String id) {
        return ResponseEntity.ok(collectionRequestService.getById(id, authentication.getName(), CallerRoles.of(authentication)));
    }

    /** Creates a request on a client's behalf; clientId is the client it's for, not the admin. */
    @PostMapping
    public ResponseEntity<CollectionRequestResponse> create(
            Authentication authentication,
            @RequestParam String clientId,
            @Valid @RequestBody CollectionRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collectionRequestService.adminCreate(authentication.getName(), clientId, request));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<CollectionRequestResponse> assign(
            Authentication authentication, @PathVariable String id, @RequestParam String collectorId) {
        return ResponseEntity.ok(collectionRequestService.adminAssign(authentication.getName(), id, collectorId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CollectionRequestResponse> updateStatus(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam CollectionStatus status,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(collectionRequestService.adminUpdateStatus(authentication.getName(), id, status, reason));
    }

    @PutMapping("/{id}/payment-status")
    public ResponseEntity<CollectionRequestResponse> updatePaymentStatus(
            Authentication authentication, @PathVariable String id, @RequestParam PaymentStatus paymentStatus) {
        return ResponseEntity.ok(collectionRequestService.adminUpdatePaymentStatus(authentication.getName(), id, paymentStatus));
    }

    @PutMapping("/{id}/quote")
    public ResponseEntity<CollectionRequestResponse> setQuote(
            Authentication authentication, @PathVariable String id, @RequestParam BigDecimal quotedPrice) {
        return ResponseEntity.ok(collectionRequestService.adminSetQuote(authentication.getName(), id, quotedPrice));
    }
}