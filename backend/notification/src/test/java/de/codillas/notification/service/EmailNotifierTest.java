package de.codillas.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailNotifier")
class EmailNotifierTest {

  @Mock private ObjectProvider<JavaMailSender> mailSenderProvider;
  @Mock private JavaMailSender mailSender;
  @Mock private TemplateEngine templateEngine;
  @Mock private RecipientEmailResolver emailResolver;

  private EmailNotifier notifier() {
    return new EmailNotifier(mailSenderProvider, templateEngine, emailResolver);
  }

  @Test
  @DisplayName("sends a rendered email when both a mail server and an address are available")
  void sends_whenConfiguredAndResolved() {
    UUID recipient = UUID.randomUUID();
    when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
    when(emailResolver.resolve(recipient)).thenReturn(Optional.of("a@b.de"));
    when(templateEngine.process(anyString(), any())).thenReturn("<html></html>");
    when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));

    notifier().send(recipient, "subj", "title", "body");

    verify(mailSender).send(any(MimeMessage.class));
  }

  @Test
  @DisplayName("skips sending when no address can be resolved")
  void skips_whenNoAddress() {
    UUID recipient = UUID.randomUUID();
    when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
    when(emailResolver.resolve(recipient)).thenReturn(Optional.empty());

    notifier().send(recipient, "subj", "title", "body");

    verify(mailSender, never()).send(any(MimeMessage.class));
  }

  @Test
  @DisplayName("skips sending when no mail server is configured")
  void skips_whenNoMailServer() {
    when(mailSenderProvider.getIfAvailable()).thenReturn(null);

    notifier().send(UUID.randomUUID(), "subj", "title", "body");

    verify(emailResolver, never()).resolve(any());
  }
}
