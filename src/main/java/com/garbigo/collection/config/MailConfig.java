package com.garbigo.collection.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Builds JavaMailSender directly from spring.mail.* via @Value, rather
 * than injecting Spring Boot's own MailProperties bean. An earlier
 * version of this class depended on that bean - in this Spring Boot
 * 4.1.1 build, it's never actually registered (mail auto-configuration
 * doesn't appear to trigger, despite spring-boot-starter-mail being on
 * the classpath and spring.mail.* resolving fine via @Value elsewhere).
 * Rather than chase that further, this just reads the same properties
 * directly - @Value resolves against the Environment regardless of
 * which auto-configuration classes did or didn't fire.
 *
 * Sole purpose: sanitize username/password against stray quotes before
 * use - a value pasted as 'app password' (single or double quotes) is
 * an easy, common .env mistake, since .env has no quoting syntax and
 * Spring Boot would otherwise send the quote characters to the SMTP
 * server as part of the literal credential (exactly what broke
 * MAIL_PASSWORD once already).
 *
 * mail.smtp.auth/starttls.enable are read individually below rather than
 * generically, since they're the only two spring.mail.properties.*
 * entries application.yml actually sets - add another @Value line here
 * if a third one is ever needed.
 */
@Configuration
public class MailConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private String smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private String smtpStarttls;

    @Bean
    JavaMailSender javaMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setProtocol("smtp");
        sender.setDefaultEncoding("UTF-8");
        sender.setUsername(stripQuotes(username));
        sender.setPassword(stripQuotes(password));

        Properties javaMailProperties = new Properties();
        javaMailProperties.put("mail.smtp.auth", smtpAuth);
        javaMailProperties.put("mail.smtp.starttls.enable", smtpStarttls);
        sender.setJavaMailProperties(javaMailProperties);

        return sender;
    }

    /**
     * Strips one matching pair of leading/trailing quotes (' or ") after
     * trimming whitespace. Mismatched quotes (one single, one double) are
     * left alone, since that's more likely a genuine typo than an attempt
     * at quoting a value.
     */
    static String stripQuotes(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2) {
            char first = trimmed.charAt(0);
            char last = trimmed.charAt(trimmed.length() - 1);
            boolean bothSingle = first == '\'' && last == '\'';
            boolean bothDouble = first == '"' && last == '"';
            if (bothSingle || bothDouble) {
                return trimmed.substring(1, trimmed.length() - 1);
            }
        }
        return trimmed;
    }
}