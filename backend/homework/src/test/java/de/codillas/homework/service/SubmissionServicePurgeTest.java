package de.codillas.homework.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmissionServiceImpl — purge on GroupDeleted")
class SubmissionServicePurgeTest {

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
  @DisplayName("deletes grades/reviews then submissions then assignments for the group")
  void cascadeDeletes() {
    UUID groupId = UUID.randomUUID();
    Assignment assignment = Assignment.builder().build();
    assignment.setId(UUID.randomUUID());
    Submission submission = Submission.builder().assignmentId(assignment.getId()).build();
    submission.setId(UUID.randomUUID());

    when(assignmentRepository.findByGroupId(groupId)).thenReturn(List.of(assignment));
    when(repository.findByAssignmentIdIn(List.of(assignment.getId())))
        .thenReturn(List.of(submission));

    service.onGroupDeleted(groupId);

    InOrder order = inOrder(gradeRepository, reviewRepository, repository, assignmentRepository);
    order.verify(gradeRepository).deleteBySubmissionIdIn(List.of(submission.getId()));
    order.verify(reviewRepository).deleteBySubmissionIdIn(List.of(submission.getId()));
    order.verify(repository).deleteByAssignmentIdIn(List.of(assignment.getId()));
    order.verify(assignmentRepository).deleteByGroupId(groupId);
  }

  @Test
  @DisplayName("does nothing when the group has no assignments")
  void noAssignmentsNoDeletes() {
    UUID groupId = UUID.randomUUID();
    when(assignmentRepository.findByGroupId(groupId)).thenReturn(List.of());

    service.onGroupDeleted(groupId);

    verify(assignmentRepository).findByGroupId(groupId);
  }
}
