package com.backend.notification.messaging;

import com.backend.common.model.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class NotificationSender {
    private static final Logger log = LoggerFactory.getLogger(NotificationSender.class);
    private final JavaMailSender mailSender;

    @Value("${notification.mail.to:test@example.com}")
    private String defaultRecipient;

    public NotificationSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOrderPaid(EventEnvelope<String> event) {
        sendEmail("Order paid", "Order paid payload: " + event.payload());
        sendSms("Order paid: " + event.payload());
    }

    public void sendOrderCreated(EventEnvelope<String> event) {
        sendEmail("Order created", "Order created payload: " + event.payload());
        sendSms("Order created: " + event.payload());
    }

    private void sendEmail(String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(defaultRecipient);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Sent email to {} subject={}", defaultRecipient, subject);
        } catch (Exception e) {
            log.error("Email send failed", e);
        }
    }

    private void sendSms(String text) {
        // Stub: replace with real SMS provider integration
        log.info("SMS stub sent: {}", text);
    }
}
