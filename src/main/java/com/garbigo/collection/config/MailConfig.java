package com.garbigo.collection.config;

import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Builds JavaMailSender manually instead of leaving it to Spring Boot's own
 * spring.mail.* auto-configuration, purely so username/password get
 * sanitized first. A value pasted with quotes around it (single or
 * double) is an easy, common mistake - .env files have no quoting syntax,
 * so Spring Boot would otherwise send the quote characters to the SMTP
 * server as part of the literal credential (exactly what broke
 * MAIL_PASSWORD once already: 'an app password' was sent quotes and all).
 *
 * Everything else (host, port, the mail.smtp.* properties map, protocol,
 * default encoding) still comes straight from the auto-configured
 * MailProperties/spring.mail.* - only username/password get this extra
 * pass. Defining this bean is what stops Spring Boot's own
 * @ConditionalOnMissingBean(JavaMailSender.class) auto-configuration from
 * also creating one - MailProperties itself still binds normally either
 * way, since that's a separate concern from the bean creation.
 */
@Configuration
public class MailConfig {

    private final MailProperties mailProperties;

    public MailConfig(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    @Bean
    JavaMailSender javaMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailProperties.getHost());
        if (mailProperties.getPort() != null) {
            sender.setPort(mailProperties.getPort());
        }
        if (mailProperties.getProtocol() != null) {
            sender.setProtocol(mailProperties.getProtocol());
        }
        if (mailProperties.getDefaultEncoding() != null) {
            sender.setDefaultEncoding(mailProperties.getDefaultEncoding().name());
        }
        sender.setUsername(stripQuotes(mailProperties.getUsername()));
        sender.setPassword(stripQuotes(mailProperties.getPassword()));

        Properties javaMailProperties = new Properties();
        javaMailProperties.putAll(mailProperties.getProperties());
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