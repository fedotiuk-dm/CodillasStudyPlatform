package de.codillas.notification.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.notification.api.dto.NotificationListResponse;
import de.codillas.notification.domain.model.Notification;
import de.codillas.notification.domain.model.NotificationType;
import de.codillas.notification.domain.repository.NotificationMembershipRepository;
import de.codillas.notification.domain.repository.NotificationRepository;
import de.codillas.notification.mapper.NotificationMapper;
import de.codillas.shared.domain.repository.GenericSpecification;
import de.codillas.shared.event.AssignmentDueSoon;
import de.codillas.shared.event.AssignmentPublished;
import de.codillas.shared.event.AttemptCompleted;
import de.codillas.shared.event.DirectMessagePosted;
import de.codillas.shared.event.GroupDeleted;
import de.codillas.shared.event.StudentEnrolled;
import de.codillas.shared.event.SubmissionGraded;
import de.codillas.shared.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository repository;
  private final NotificationMembershipRepository membershipRepository;
  private final NotificationMapper mapper;
  private final EmailNotifier emailNotifier;

  @Override
  public NotificationListResponse listMyNotifications(UUID userId, Pageable pageable) {
    return mapper.toListResponse(
        repository.findByRecipientId(
            userId,
            GenericSpecification.withDefaultSort(pageable, NotificationRepository.NEWEST_FIRST)),
        repository.countByRecipientIdAndReadFalse(userId));
  }

  @Override
  @Transactional
  public void markRead(UUID userId, UUID notificationId) {
    Notification notification =
        repository
            .findByIdAndRecipientId(notificationId, userId)
            .orElseThrow(() -> new NotFoundException("Notification", notificationId));
    notification.setRead(true);
    repository.save(notification);
  }

  @Override
  @Transactional
  public void onGroupDeleted(GroupDeleted event) {
    membershipRepository.deleteByGroupId(event.groupId());
  }

  @Override
  @Transactional
  public void onStudentEnrolled(StudentEnrolled event) {
    if (!membershipRepository.existsByGroupIdAndStudentId(event.groupId(), event.userId())) {
      membershipRepository.save(mapper.toMembership(event));
    }
  }

  @Override
  @Transactional
  public void onAssignmentPublished(AssignmentPublished event) {
    membershipRepository
        .findByGroupId(event.groupId())
        .forEach(
            member ->
                notify(
                    member.getStudentId(),
                    NotificationType.ASSIGNMENT_PUBLISHED,
                    "New assignment",
                    "A new assignment was published for your group.",
                    event.assignmentId()));
  }

  @Override
  @Transactional
  public void onAssignmentDueSoon(AssignmentDueSoon event) {
    membershipRepository
        .findByGroupId(event.groupId())
        .forEach(
            member ->
                notify(
                    member.getStudentId(),
                    NotificationType.ASSIGNMENT_DUE_SOON,
                    "Assignment due soon",
                    "An assignment in your group is due soon.",
                    event.assignmentId()));
  }

  @Override
  @Transactional
  public void onSubmissionGraded(SubmissionGraded event) {
    notify(
        event.studentId(),
        NotificationType.SUBMISSION_GRADED,
        "Homework graded",
        "Your submission was graded: " + event.awarded() + " points.",
        event.submissionId());
  }

  @Override
  @Transactional
  public void onAttemptCompleted(AttemptCompleted event) {
    notify(
        event.studentId(),
        NotificationType.ATTEMPT_COMPLETED,
        "Test scored",
        "Your test attempt scored " + event.awarded() + " points.",
        event.attemptId());
  }

  @Override
  @Transactional
  public void onDirectMessage(DirectMessagePosted event) {
    notify(
        event.recipientId(),
        NotificationType.DIRECT_MESSAGE,
        "New message",
        "You have a new direct message.",
        event.roomId());
  }

  private void notify(
      UUID recipientId, NotificationType type, String title, String body, UUID referenceId) {
    repository.save(mapper.toNotification(recipientId, type, title, body, referenceId));
    emailNotifier.send(recipientId, title, title, body);
  }
}
