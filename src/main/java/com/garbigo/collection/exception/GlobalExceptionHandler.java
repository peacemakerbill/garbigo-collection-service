package com.garbigo.collection.exception;

import com.garbigo.collection.dto.MessageResponse;
import feign.RetryableException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final Pattern QUOTED_VALUE = Pattern.compile("'([^']+)'");

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<MessageResponse> handleCustomException(CustomException ex) {
        return ResponseEntity.badRequest().body(new MessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining(" "));
        return ResponseEntity.badRequest().body(new MessageResponse(message));
    }

    /**
     * Method-level security (@PreAuthorize) throws this - an
     * AuthorizationDeniedException, a subclass - from inside the controller
     * call, so it reaches this advice before Spring Security's own filter
     * chain ever sees it, and without a handler here the catch-all below
     * turned every wrong-role request into a generic 500. Names both the
     * caller's role and the role(s) the endpoint actually requires (read
     * off its @PreAuthorize), so the message says what to do about it.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<MessageResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        List<String> currentRoles = currentRoles();
        List<String> requiredRoles = requiredRoles(request);

        log.info("Access denied: {} {} (caller roles: {}, endpoint requires: {})",
                request.getMethod(), request.getRequestURI(),
                currentRoles.isEmpty() ? "none" : currentRoles,
                requiredRoles.isEmpty() ? "unknown" : requiredRoles);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse(accessDeniedMessage(currentRoles, requiredRoles)));
    }

    // Feign throws this specifically for connection failures (a downstream
    // service unreachable), not for a normal error response it actually
    // returned - that distinction is what keeps this from also swallowing
    // real 404s/etc. Covers both auth-service and wallet-service now.
    @ExceptionHandler(RetryableException.class)
    public ResponseEntity<MessageResponse> handleServiceUnavailable(RetryableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new MessageResponse("A required service is temporarily unavailable. Please try again in a moment."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleException(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.internalServerError()
                .body(new MessageResponse("Something went wrong on our end. Please try again later."));
    }

    private String accessDeniedMessage(List<String> currentRoles, List<String> requiredRoles) {
        if (currentRoles.isEmpty()) {
            // Authenticated (valid JWT) but no role resolved for this user -
            // JwtFilter couldn't find them in the user cache or reach
            // auth-service to refresh it. Different problem from "wrong role".
            return "Your account's role hasn't been recognised yet, so this action can't be authorised. "
                    + "Please try again in a moment, or contact support if it keeps happening.";
        }

        String current = String.join(", ", currentRoles);
        if (requiredRoles.isEmpty()) {
            return "You're signed in as " + current + ", which isn't permitted to perform this action.";
        }
        return "You're signed in as " + current + ", but this action is only available to "
                + String.join(" or ", requiredRoles) + " accounts.";
    }

    private List<String> currentRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return List.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .toList();
    }

    /**
     * Reads the role(s) off the @PreAuthorize on the endpoint that was
     * called (method first, then class - AdminController gates at class
     * level). Only understands hasRole('X') / hasAnyRole('X', 'Y');
     * anything fancier returns empty and the caller falls back to a
     * message that doesn't name a required role, rather than guessing.
     */
    private List<String> requiredRoles(HttpServletRequest request) {
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return List.of();
        }

        PreAuthorize annotation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), PreAuthorize.class);
        if (annotation == null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), PreAuthorize.class);
        }
        if (annotation == null) {
            return List.of();
        }

        String expression = annotation.value();
        if (!expression.contains("hasRole(") && !expression.contains("hasAnyRole(")) {
            return List.of();
        }

        List<String> roles = new ArrayList<>();
        Matcher matcher = QUOTED_VALUE.matcher(expression);
        while (matcher.find()) {
            String role = matcher.group(1);
            roles.add(role.startsWith(ROLE_PREFIX) ? role.substring(ROLE_PREFIX.length()) : role);
        }
        return roles;
    }
}