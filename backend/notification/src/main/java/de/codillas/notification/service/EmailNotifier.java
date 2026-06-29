package de.codillas.notification.service;

import java.util.Optional;
import java.util.UUID;

import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import de.codillas.notification.config.NotificationProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Renders a Thymeleaf email and sends it via the mail server. Both the mail server (only present
 * when {@code spring.mail.*} is configured — e.g. mailpit in dev) and the recipient address are
 * optional: when either is missing, email is skipped and the in-app notification still stands.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotifier {

  private static final String TEMPLATE = "email/notification";

  private final ObjectProvider<JavaMailSender> mailSenderProvider;
  private final TemplateEngine templateEngine;
  private final RecipientEmailResolver emailResolver;
  private final NotificationProperties notificationProperties;

  // Off the caller's thread so blocking SMTP never holds the event-listener's DB transaction open.
  @Async
  public void send(UUID recipientId, String subject, String title, String body) {
    JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
    if (mailSender == null) {
      return;
    }
    Optional<String> address = emailResolver.resolve(recipientId);
    if (address.isEmpty()) {
      log.debug(
          "No email address for recipient {} — skipping email for '{}'", recipientId, subject);
      return;
    }
    try {
      Context context = new Context();
      context.setVariable("title", title);
      context.setVariable("body", body);
      String html = templateEngine.process(TEMPLATE, context);

      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
      helper.setFrom(notificationProperties.getFrom());
      helper.setTo(address.get());
      helper.setSubject(subject);
      helper.setText(html, true);
      mailSender.send(message);
    } catch (Exception e) {
      log.warn(
          "Failed to send notification email to recipient {}: {}", recipientId, e.getMessage());
    }
  }
}
