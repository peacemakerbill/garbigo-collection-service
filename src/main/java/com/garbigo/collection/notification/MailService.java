package com.garbigo.collection.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** Sends branded HTML emails from templates under resources/templates/email/. */
@Service
@Slf4j
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
            String locationSummary) {

        Map<String, Object> variables = Map.of(
                "recipientName", displayName(recipientName),
                "requestId", requestId,
                "wasteType", wasteType,
                "scheduledAt", scheduledAt,
                "locationSummary", locationSummary
        );

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

    private String displayName(String recipientName) {
        return (recipientName == null || recipientName.isBlank()) ? "there" : recipientName;
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
            log.warn("Failed to send '{}' email to {}: {}", templateName, toEmail, e.getMessage());
        }
    }
}