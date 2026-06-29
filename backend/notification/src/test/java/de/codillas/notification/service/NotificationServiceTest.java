package de.codillas.notification.service;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import de.codillas.notification.config.NotificationProperties;
import de.codillas.notification.config.NotificationProperties.Template;
import de.codillas.notification.domain.model.Notification;
import de.codillas.notification.domain.model.NotificationMembership;
import de.codillas.notification.domain.model.NotificationType;
import de.codillas.notification.domain.repository.NotificationMembershipRepository;
import de.codillas.notification.domain.repository.NotificationRepository;
import de.codillas.notification.mapper.NotificationMapper;
import de.codillas.shared.event.AssignmentDueSoon;
import de.codillas.shared.event.AssignmentPublished;
import de.codillas.shared.event.DirectMessagePosted;
import de.codillas.shared.event.SubmissionGraded;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService")
class NotificationServiceTest {

  @Mock private NotificationRepository repository;
  @Mock private NotificationMembershipRepository membershipRepository;
  @Mock private NotificationMapper mapper;
  @Mock private EmailNotifier emailNotifier;

  private NotificationServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new NotificationServiceImpl(
            repository, membershipRepository, mapper, emailNotifier, resolver());
  }

  // Real resolver rendering English templates, so the asserted text exercises substitution too.
  private static NotificationTemplateResolver resolver() {
    NotificationProperties properties = new NotificationProperties();
    properties.setDefaultLocale("en");
    properties.setTemplates(
        Map.of(
            "en",
            Map.of(
                NotificationType.ASSIGNMENT_PUBLISHED,
                new Template("New assignment", "A new assignment was published for your group."),
                NotificationType.ASSIGNMENT_DUE_SOON,
                new Template("Assignment due soon", "An assignment in your group is due soon."),
                NotificationType.SUBMISSION_GRADED,
                new Template("Homework graded", "Your submission was graded: {points} points."),
                NotificationType.ATTEMPT_COMPLETED,
                new Template("Test scored", "Your test attempt scored {points} points."),
                NotificationType.DIRECT_MESSAGE,
                new Template("New message", "You have a new chat message."))));
    return new NotificationTemplateResolver(properties);
  }

  @Test
  @DisplayName("onSubmissionGraded notifies the student in-app and by email with the rendered text")
  void onSubmissionGraded_notifiesStudent() {
    UUID submissionId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    SubmissionGraded event =
        new SubmissionGraded(
            submissionId, UUID.randomUUID(), studentId, 80, 100, UUID.randomUUID());
    when(mapper.toNotification(
            eq(studentId),
            eq(NotificationType.SUBMISSION_GRADED),
            eq("Homework graded"),
            eq("Your submission was graded: 80 points."),
            eq(submissionId)))
        .thenReturn(new Notification());

    service.onSubmissionGraded(event);

    verify(repository).save(any(Notification.class));
    verify(emailNotifier)
        .send(
            eq(studentId),
            eq("Homework graded"),
            eq("Homework graded"),
            eq("Your submission was graded: 80 points."));
  }

  @Test
  @DisplayName("onAssignmentPublished fans out to every group member")
  void onAssignmentPublished_fansOut() {
    UUID groupId = UUID.randomUUID();
    UUID assignmentId = UUID.randomUUID();
    AssignmentPublished event = new AssignmentPublished(assignmentId, groupId);
    when(membershipRepository.findByGroupId(groupId))
        .thenReturn(List.of(member(UUID.randomUUID()), member(UUID.randomUUID())));
    when(mapper.toNotification(any(), any(), anyString(), anyString(), any()))
        .thenReturn(new Notification());

    service.onAssignmentPublished(event);

    verify(repository, times(2)).save(any(Notification.class));
    verify(emailNotifier, times(2))
        .send(
            any(),
            eq("New assignment"),
            eq("New assignment"),
            eq("A new assignment was published for your group."));
  }

  @Test
  @DisplayName("onAssignmentDueSoon reminds every group member in-app and by email")
  void onAssignmentDueSoon_remindsMembers() {
    UUID groupId = UUID.randomUUID();
    UUID assignmentId = UUID.randomUUID();
    AssignmentDueSoon event = new AssignmentDueSoon(assignmentId, groupId, java.time.Instant.now());
    when(membershipRepository.findByGroupId(groupId))
        .thenReturn(List.of(member(UUID.randomUUID()), member(UUID.randomUUID())));
    when(mapper.toNotification(
            any(),
            eq(NotificationType.ASSIGNMENT_DUE_SOON),
            eq("Assignment due soon"),
            eq("An assignment in your group is due soon."),
            eq(assignmentId)))
        .thenReturn(new Notification());

    service.onAssignmentDueSoon(event);

    verify(repository, times(2)).save(any(Notification.class));
    verify(emailNotifier, times(2))
        .send(
            any(),
            eq("Assignment due soon"),
            eq("Assignment due soon"),
            eq("An assignment in your group is due soon."));
  }

  @Test
  @DisplayName("onDirectMessage notifies the recipient in-app and by email")
  void onDirectMessage_notifiesRecipient() {
    UUID roomId = UUID.randomUUID();
    UUID recipientId = UUID.randomUUID();
    UUID senderId = UUID.randomUUID();
    DirectMessagePosted event = new DirectMessagePosted(roomId, recipientId, senderId);
    when(mapper.toNotification(
            eq(recipientId),
            eq(NotificationType.DIRECT_MESSAGE),
            eq("New message"),
            eq("You have a new chat message."),
            eq(roomId)))
        .thenReturn(new Notification());

    service.onDirectMessage(event);

    verify(repository).save(any(Notification.class));
    verify(emailNotifier)
        .send(
            eq(recipientId),
            eq("New message"),
            eq("New message"),
            eq("You have a new chat message."));
  }

  @Test
  @DisplayName("markRead flags the recipient's notification as read")
  void markRead_flagsRead() {
    UUID userId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();
    Notification notification = new Notification();
    when(repository.findByIdAndRecipientId(notificationId, userId))
        .thenReturn(Optional.of(notification));

    service.markRead(userId, notificationId);

    verify(repository).save(notification);
    org.assertj.core.api.Assertions.assertThat(notification.isRead()).isTrue();
  }

  @Test
  @DisplayName("markRead on someone else's (or missing) notification is a 404")
  void markRead_notFound() {
    UUID userId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();
    when(repository.findByIdAndRecipientId(notificationId, userId)).thenReturn(Optional.empty());
    assertThatExceptionOfType(de.codillas.shared.exception.NotFoundException.class)
        .isThrownBy(() -> service.markRead(userId, notificationId));
  }

  @Test
  @DisplayName("onStudentEnrolled is idempotent for an existing roster entry")
  void onStudentEnrolled_idempotent() {
    de.codillas.shared.event.StudentEnrolled event =
        new de.codillas.shared.event.StudentEnrolled(UUID.randomUUID(), UUID.randomUUID());
    when(membershipRepository.existsByGroupIdAndStudentId(event.groupId(), event.userId()))
        .thenReturn(true);

    service.onStudentEnrolled(event);

    verify(membershipRepository, never()).save(any());
  }

  private static NotificationMembership member(UUID studentId) {
    return NotificationMembership.builder().studentId(studentId).build();
  }
}
