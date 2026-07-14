package de.codillas.homework.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import de.codillas.homework.domain.SubmissionStateMachine;
import de.codillas.homework.domain.model.Assignment;
import de.codillas.homework.domain.model.Submission;
import de.codillas.homework.domain.repository.AssignmentRepository;
import de.codillas.homework.domain.repository.GradeRepository;
import de.codillas.homework.domain.repository.ReviewRepository;
import de.codillas.homework.domain.repository.SubmissionRepository;
import de.codillas.homework.mapper.GradeMapper;
import de.codillas.homework.mapper.ReviewMapper;
import de.codillas.homework.mapper.SubmissionMapper;
import de.codillas.shared.security.CurrentUser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmissionServiceImpl — late flag at submit")
class SubmissionServiceLateTest {

  @Mock SubmissionRepository repository;
  @Mock ReviewRepository reviewRepository;
  @Mock GradeRepository gradeRepository;
  @Mock AssignmentRepository assignmentRepository;
  @Mock SubmissionMapper mapper;
  @Mock ReviewMapper reviewMapper;
  @Mock GradeMapper gradeMapper;
  @Mock SubmissionStateMachine stateMachine;
  @Mock CurrentUser currentUser;
  @Mock ApplicationEventPublisher events;
  @InjectMocks SubmissionServiceImpl service;

  @Test
  @DisplayName("marks the submission late when submitted after the due date")
  void marksLate() {
    UUID id = UUID.randomUUID();
    UUID assignmentId = UUID.randomUUID();
    UUID student = UUID.randomUUID();
    Submission submission =
        Submission.builder().assignmentId(assignmentId).studentId(student).build();
    Assignment past = Assignment.builder().dueAt(Instant.now().minusSeconds(3600)).build();
    when(repository.findById(id)).thenReturn(Optional.of(submission));
    when(currentUser.id()).thenReturn(student);
    when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(past));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.submitSubmission(id);

    assertThat(submission.isLate()).isTrue();
    assertThat(submission.getSubmittedAt()).isNotNull();
  }

  @Test
  @DisplayName("does not 404 (flag-don't-block) and is not late when the assignment row is absent")
  void missingAssignmentNotLate() {
    UUID id = UUID.randomUUID();
    UUID assignmentId = UUID.randomUUID();
    UUID student = UUID.randomUUID();
    Submission submission =
        Submission.builder().assignmentId(assignmentId).studentId(student).build();
    when(repository.findById(id)).thenReturn(Optional.of(submission));
    when(currentUser.id()).thenReturn(student);
    when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.submitSubmission(id);

    assertThat(submission.isLate()).isFalse();
    assertThat(submission.getSubmittedAt()).isNotNull();
  }

  @Test
  @DisplayName("does not mark late when there is no due date")
  void noDueDateNotLate() {
    UUID id = UUID.randomUUID();
    UUID assignmentId = UUID.randomUUID();
    UUID student = UUID.randomUUID();
    Submission submission =
        Submission.builder().assignmentId(assignmentId).studentId(student).build();
    when(repository.findById(id)).thenReturn(Optional.of(submission));
    when(currentUser.id()).thenReturn(student);
    when(assignmentRepository.findById(assignmentId))
        .thenReturn(Optional.of(Assignment.builder().build()));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.submitSubmission(id);

    assertThat(submission.isLate()).isFalse();
  }
}
