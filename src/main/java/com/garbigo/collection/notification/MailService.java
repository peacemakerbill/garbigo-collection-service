package com.garbigo.collection.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends branded HTML emails from templates under resources/templates/email/.
 *
 * @Async at class level - every public method here is a fire-and-forget
 * notification (none of their callers use a return value or need to wait
 * for the send to finish), so a request like creating a collection
 * request no longer blocks on an actual SMTP round-trip before
 * responding. Runs on the bounded pool in AsyncConfig, not Spring's
 * unbounded default. A send failure is still caught and logged inside
 * send() itself exactly as before - that didn't change, and still
 * doesn't propagate anywhere there'd be something to catch it now that
 * it's off-thread.
 */
@Service
@Slf4j
@Async("mailTaskExecutor")
public class MailService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm 'UTC'");

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final String fromAddress;
    private final String appName;
    private final String appUrl;
    private final String supportEmail;
    private final String companyName;
    private final String companyAddress;

    public MailService(
            JavaMailSender javaMailSender,
            TemplateEngine templateEngine,
            @Value("${spring.mail.username}") String fromAddress,
            @Value("${app.name}") String appName,
            @Value("${app.url}") String appUrl,
            @Value("${app.support-email}") String supportEmail,
            @Value("${app.company.name}") String companyName,
            @Value("${app.company.address}") String companyAddress) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
        this.fromAddress = fromAddress;
        this.appName = appName;
        this.appUrl = appUrl;
        this.supportEmail = supportEmail;
        this.companyName = companyName;
        this.companyAddress = companyAddress;
    }

    public void sendCollectionRequestConfirmation(
            String toEmail,
            String recipientName,
            String requestId,
            String wasteType,
            String scheduledAt,
            String locationSummary,
            BigDecimal quotedPrice,
            String currency) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("recipientName", displayName(recipientName));
        variables.put("requestId", requestId);
        variables.put("wasteType", wasteType);
        variables.put("scheduledAt", scheduledAt);
        variables.put("locationSummary", locationSummary);
        putPrice(variables, quotedPrice, currency);

        send(toEmail, "Your Garbigo pickup is confirmed", "email/collection-request-confirmation", variables);
    }

    public void sendComplaintResolved(
            String toEmail,
            String recipientName,
            String complaintId,
            String complaintDescription) {

        Map<String, Object> variables = Map.of(
                "recipientName", displayName(recipientName),
                "complaintId", complaintId,
                "complaintDescription", complaintDescription
        );

        send(toEmail, "Your Garbigo complaint has been resolved", "email/complaint-resolved", variables);
    }

    public void sendSewageRequestConfirmation(
            String toEmail,
            String recipientName,
            String requestId,
            String tankVolumeLiters,
            String urgency,
            String scheduledAt,
            String locationSummary,
            BigDecimal quotedPrice,
            String currency) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("recipientName", displayName(recipientName));
        variables.put("requestId", requestId);
        variables.put("tankVolumeLiters", tankVolumeLiters);
        variables.put("urgency", urgency);
        variables.put("scheduledAt", scheduledAt);
        variables.put("locationSummary", locationSummary);
        putPrice(variables, quotedPrice, currency);

        send(toEmail, "Your Garbigo exhauster request is confirmed", "email/sewage-request-confirmation", variables);
    }

    public void sendUpcomingPickupReminder(
            String toEmail,
            String recipientName,
            String requestId,
            String wasteType,
            String scheduledAt,
            String locationSummary) {

        Map<String, Object> variables = Map.of(
                "recipientName", displayName(recipientName),
                "requestId", requestId,
                "wasteType", wasteType,
                "scheduledAt", scheduledAt,
                "locationSummary", locationSummary
        );

        send(toEmail, "Reminder: your Garbigo pickup is tomorrow", "email/upcoming-pickup-reminder", variables);
    }

    /** Sent to the client when a collector is assigned - the email equivalent of what an SMS ping would have covered. */
    public void sendCollectorAssignedToClient(
            String toEmail,
            String recipientName,
            String requestId,
            String serviceType,
            String collectorName,
            String scheduledAt,
            String locationSummary,
            BigDecimal quotedPrice,
            String currency) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("recipientName", displayName(recipientName));
        variables.put("requestId", requestId);
        variables.put("serviceType", serviceType);
        variables.put("collectorName", collectorName);
        variables.put("scheduledAt", scheduledAt);
        variables.put("locationSummary", locationSummary);
        putPrice(variables, quotedPrice, currency);

        send(toEmail, "A collector has been assigned to your " + serviceType.toLowerCase() + " request",
                "email/collector-assigned-client", variables);
    }

    /** Sent to the collector when a client assigns them a job. */
    public void sendJobAssignedToCollector(
            String toEmail,
            String recipientName,
            String requestId,
            String serviceType,
            String scheduledAt,
            String locationSummary,
            BigDecimal quotedPrice,
            String currency) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("recipientName", displayName(recipientName));
        variables.put("requestId", requestId);
        variables.put("serviceType", serviceType);
        variables.put("scheduledAt", scheduledAt);
        variables.put("locationSummary", locationSummary);
        putPrice(variables, quotedPrice, currency);

        send(toEmail, "New " + serviceType + " job assigned to you", "email/collector-assigned-collector", variables);
    }

    /** Sent to the client when their assigned collector declines (e.g. price too low) - request has reverted to PENDING. */
    public void sendCollectorDeclined(
            String toEmail,
            String recipientName,
            String requestId,
            String serviceType,
            String collectorName,
            String reason,
            BigDecimal quotedPrice,
            String currency) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("recipientName", displayName(recipientName));
        variables.put("requestId", requestId);
        variables.put("serviceType", serviceType);
        variables.put("collectorName", collectorName);
        variables.put("reason", reason == null || reason.isBlank() ? "No reason given" : reason);
        putPrice(variables, quotedPrice, currency);

        send(toEmail, "Your " + serviceType.toLowerCase() + " request needs a new collector",
                "email/collector-declined", variables);
    }

    /** Sent to the client when a request's status becomes COMPLETED. showRatingPrompt invites a rating - true for both kinds of request now. */
    public void sendRequestCompleted(
            String toEmail,
            String recipientName,
            String requestId,
            String serviceType,
            boolean showRatingPrompt,
            BigDecimal quotedPrice,
            String currency) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("recipientName", displayName(recipientName));
        variables.put("requestId", requestId);
        variables.put("serviceType", serviceType);
        variables.put("showRatingPrompt", showRatingPrompt);
        putPrice(variables, quotedPrice, currency);

        send(toEmail, "Your " + serviceType.toLowerCase() + " is complete", "email/request-completed", variables);
    }

    /** Sent to the client when the collector they were offered confirms they're taking the job. */
    public void sendCollectorAccepted(
            String toEmail,
            String recipientName,
            String requestId,
            String serviceType,
            String collectorName,
            String scheduledAt,
            String locationSummary) {

        Map<String, Object> variables = Map.of(
                "recipientName", displayName(recipientName),
                "requestId", requestId,
                "serviceType", serviceType,
                "collectorName", collectorName,
                "scheduledAt", scheduledAt,
                "locationSummary", locationSummary
        );

        send(toEmail, "Your collector has accepted your " + serviceType.toLowerCase() + " request",
                "email/collector-accepted", variables);
    }

    /** Sent to the collector when the client disputes a job they marked completed. */
    public void sendJobDisputed(
            String toEmail,
            String recipientName,
            String requestId,
            String serviceType,
            String reason) {

        Map<String, Object> variables = Map.of(
                "recipientName", displayName(recipientName),
                "requestId", requestId,
                "serviceType", serviceType,
                "reason", reason == null || reason.isBlank() ? "No reason given" : reason
        );

        send(toEmail, "A client disputed your " + serviceType.toLowerCase() + " job",
                "email/job-disputed", variables);
    }

    /** Sent to the collector when the client changes the date, place or details of a job they hold. */
    public void sendJobUpdatedToCollector(
            String toEmail,
            String recipientName,
            String requestId,
            String serviceType,
            String scheduledAt,
            String locationSummary) {

        Map<String, Object> variables = Map.of(
                "recipientName", displayName(recipientName),
                "requestId", requestId,
                "serviceType", serviceType,
                "scheduledAt", scheduledAt,
                "locationSummary", locationSummary
        );

        send(toEmail, "A " + serviceType.toLowerCase() + " job you hold has changed",
                "email/job-updated-collector", variables);
    }

    private String displayName(String recipientName) {
        return (recipientName == null || recipientName.isBlank()) ? "there" : recipientName;
    }

    /**
     * Only adds the price/currency variables when a price is actually set -
     * quotedPrice is optional on a request right up until someone sets it
     * (create, updateQuote), so templates guard display with
     * th:if="${quotedPrice != null}" rather than assuming it's always there.
     */
    private void putPrice(Map<String, Object> variables, BigDecimal quotedPrice, String currency) {
        if (quotedPrice != null) {
            variables.put("quotedPrice", quotedPrice.setScale(2, RoundingMode.HALF_UP).toPlainString());
            variables.put("currency", currency);
        }
    }

    private void send(String toEmail, String subject, String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        context.setVariable("appName", appName);
        context.setVariable("appUrl", appUrl);
        context.setVariable("companyName", companyName);
        context.setVariable("companyAddress", companyAddress);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("sentAt", TIMESTAMP_FORMAT.format(OffsetDateTime.now()));
        context.setVariable("year", String.valueOf(OffsetDateTime.now().getYear()));

        String html = templateEngine.process(templateName, context);

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setFrom(fromAddress);
            helper.setSubject(subject);
            helper.setText(html, true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            // Thrown building the message itself (bad address, etc.)
            log.warn("Failed to send '{}' email to {}: {}", templateName, toEmail, e.getMessage());
        } catch (MailException e) {
            // javaMailSender.send(...) throws Spring's own unchecked MailException
            // hierarchy on send failure (auth, connection, etc.) - NOT
            // MessagingException. Missing this catch is why a bad SMTP
            // credential was crashing the whole request instead of just
            // failing to send the email, defeating the entire point of
            // wrapping this in a try/catch in the first place.
            log.warn("Failed to send '{}' email to {}: {}", templateName, toEmail, e.getMessage());
        }
    }
}