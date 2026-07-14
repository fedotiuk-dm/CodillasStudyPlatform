package de.codillas.notification.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import de.codillas.notification.domain.model.NotificationType;

import lombok.Getter;
import lombok.Setter;

/**
 * i18n notification message templates (the "boosting" pattern): titles/bodies live in {@code
 * notification.*} config, not hardcoded in Java. {@link #defaultLocale} renders both the stored
 * in-app text and the email; per-recipient locale is a future follow-up. {@code {placeholder}}
 * tokens in a template are substituted at render time.
 *
 * <p>{@link #templates} is keyed first by locale string ({@code uk}/{@code en}/{@code de}) then by
 * {@link NotificationType} (Spring relaxed-binds e.g. {@code submission-graded} → {@code
 * SUBMISSION_GRADED}).
 */
@Getter
@Setter
@ConfigurationProperties("notification")
public class NotificationProperties {

  /** Locale used to render notifications until per-recipient locale is wired. From config. */
  private String defaultLocale;

  /** {@code From} address on outgoing notification emails. From config. */
  private String from;

  /** Per-locale, per-type message templates. */
  private Map<String, Map<NotificationType, Template>> templates = new HashMap<>();

  /** A single notification's title and body for one locale + type. */
  public record Template(String title, String body) {}
}
