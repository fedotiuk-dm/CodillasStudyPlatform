package de.codillas.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import de.codillas.notification.config.NotificationProperties;
import de.codillas.notification.config.NotificationProperties.Template;
import de.codillas.notification.domain.model.NotificationType;
import de.codillas.notification.service.NotificationTemplateResolver.Rendered;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NotificationTemplateResolver")
class NotificationTemplateResolverTest {

  private static NotificationTemplateResolver resolver(String defaultLocale) {
    NotificationProperties properties = new NotificationProperties();
    properties.setDefaultLocale(defaultLocale);
    properties.setTemplates(
        Map.of(
            "en",
            Map.of(
                NotificationType.SUBMISSION_GRADED,
                new Template("Homework graded", "Your submission was graded: {points} points."),
                NotificationType.DIRECT_MESSAGE,
                new Template("New message", "You have a new chat message.")),
            "uk",
            Map.of(
                NotificationType.SUBMISSION_GRADED,
                new Template("Роботу оцінено", "Вашу роботу оцінено: {points} балів."))));
    return new NotificationTemplateResolver(properties);
  }

  @Test
  @DisplayName("substitutes every {placeholder} with the matching param")
  void substitutesPlaceholders() {
    Rendered rendered =
        resolver("en").render(NotificationType.SUBMISSION_GRADED, Map.of("points", "85"));

    assertThat(rendered.title()).isEqualTo("Homework graded");
    assertThat(rendered.body()).isEqualTo("Your submission was graded: 85 points.");
  }

  @Test
  @DisplayName("renders in the configured default locale")
  void rendersDefaultLocale() {
    Rendered rendered =
        resolver("uk").render(NotificationType.SUBMISSION_GRADED, Map.of("points", "12"));

    assertThat(rendered.title()).isEqualTo("Роботу оцінено");
    assertThat(rendered.body()).isEqualTo("Вашу роботу оцінено: 12 балів.");
  }

  @Test
  @DisplayName("leaves a template without placeholders untouched")
  void noPlaceholders() {
    Rendered rendered = resolver("en").render(NotificationType.DIRECT_MESSAGE, Map.of());

    assertThat(rendered.title()).isEqualTo("New message");
    assertThat(rendered.body()).isEqualTo("You have a new chat message.");
  }

  @Test
  @DisplayName("falls back to en when the default locale lacks the type")
  void fallsBackToEn() {
    // uk has no DIRECT_MESSAGE template above → falls back to en.
    Rendered rendered = resolver("uk").render(NotificationType.DIRECT_MESSAGE, Map.of());

    assertThat(rendered.title()).isEqualTo("New message");
    assertThat(rendered.body()).isEqualTo("You have a new chat message.");
  }

  @Test
  @DisplayName("never NPEs when no template exists for a type — uses the type name")
  void fallsBackToTypeName() {
    Rendered rendered = resolver("en").render(NotificationType.ASSIGNMENT_PUBLISHED, Map.of());

    assertThat(rendered.title()).isEqualTo(NotificationType.ASSIGNMENT_PUBLISHED.name());
    assertThat(rendered.body()).isEqualTo(NotificationType.ASSIGNMENT_PUBLISHED.name());
  }
}
