package com.docsaas.api.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * Sends transactional email over SMTP (spring.mail.*). Mail goes out on a background thread so the
 * HTTP response time never depends on the mail server, and never reveals whether an account exists.
 *
 * With no MAIL_HOST configured (development), messages are written to the log instead.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> senderProvider;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "mail-sender");
        t.setDaemon(true);
        return t;
    });

    @Value("${spring.mail.host:}")
    private String host;

    @Value("${docsaas.mail.from}")
    private String from;

    public MailService(ObjectProvider<JavaMailSender> senderProvider) {
        this.senderProvider = senderProvider;
    }

    public boolean isConfigured() {
        return host != null && !host.isBlank() && senderProvider.getIfAvailable() != null;
    }

    /** Queues one email with an HTML body and a plain-text alternative. */
    public void sendAsync(String to, String subject, String text, String html) {
        if (!isConfigured()) {
            log.warn("MAIL_HOST is not set, so this email was not sent. To: {} | Subject: {}\n{}", to, subject, text);
            return;
        }
        executor.submit(() -> {
            try {
                JavaMailSender sender = senderProvider.getObject();
                MimeMessage message = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                InternetAddress[] parsed = InternetAddress.parse(from);
                helper.setFrom(parsed.length > 0 ? parsed[0] : new InternetAddress(from));
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(text, html);
                sender.send(message);
                log.info("Email sent: subject='{}'", subject);
            } catch (Exception e) {
                // Never log the body: it can contain a reset link
                log.error("Email could not be sent (subject='{}'): {}", subject, e.getMessage());
            }
        });
    }

    /** Logs at startup whether the SMTP settings work, so a bad password shows up before a user needs a reset. */
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void checkConnection() {
        if (!isConfigured()) {
            log.warn("Email is not configured (MAIL_HOST empty): password reset is hidden in the app and links are logged instead.");
            return;
        }
        executor.submit(() -> {
            try {
                if (senderProvider.getObject() instanceof org.springframework.mail.javamail.JavaMailSenderImpl impl) {
                    impl.testConnection();
                }
                log.info("SMTP connection OK ({}), sending as {}", host, from);
            } catch (Exception e) {
                log.error("SMTP login FAILED for {}: {}. Check MAIL_USERNAME / MAIL_PASSWORD (Gmail needs an App password).",
                        host, e.getMessage());
            }
        });
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
