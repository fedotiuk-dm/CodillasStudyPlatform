package de.codillas.notification.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import de.codillas.notification.config.NotificationProperties;
import de.codillas.notification.config.NotificationProperties.Template;
import de.codillas.notification.domain.model.NotificationType;

import lombok.RequiredArgsConstructor;

/**
 * Renders a notification's title + body from the configured per-locale templates, substituting
 * every {@code {key}} token with {@code params.get(key)}. Resolution never NPEs: it falls back from
 * the configured default locale to {@code en}, then to a plain {@link NotificationType#name()}.
 */
@Component
@RequiredArgsConstructor
public class NotificationTemplateResolver {

  private static final String FALLBACK_LOCALE = "en";

  private final NotificationProperties properties;

  /** A rendered notification's title and body. */
  public record Rendered(String title, String body) {}

  public Rendered render(NotificationType type, Map<String, String> params) {
    Template template = resolveTemplate(type);
    return new Rendered(substitute(template.title(), params), substitute(template.body(), params));
  }

  private Template resolveTemplate(NotificationType type) {
    Template template = lookup(properties.getDefaultLocale(), type);
    if (template == null) {
      template = lookup(FALLBACK_LOCALE, type);
    }
    return template != null ? template : new Template(type.name(), type.name());
  }

  private Template lookup(String locale, NotificationType type) {
    if (locale == null) {
      return null;
    }
    Map<NotificationType, Template> byType = properties.getTemplates().get(locale);
    return byType == null ? null : byType.get(type);
  }

  private static String substitute(String text, Map<String, String> params) {
    String result = text;
    for (Map.Entry<String, String> param : params.entrySet()) {
      result = result.replace("{" + param.getKey() + "}", param.getValue());
    }
    return result;
  }
}
