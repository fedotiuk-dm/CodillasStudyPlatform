package de.codillas.gradebook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.codillas.gradebook.api.dto.StudentGradebookResponse;
import de.codillas.gradebook.domain.model.GradeSource;
import de.codillas.gradebook.domain.model.ProgressEntry;
import de.codillas.gradebook.domain.repository.GradebookMembershipRepository;
import de.codillas.gradebook.domain.repository.ProgressEntryRepository;
import de.codillas.gradebook.mapper.GradebookMapper;
import de.codillas.shared.event.StudentEnrolled;
import de.codillas.shared.event.SubmissionGraded;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GradebookService")
class GradebookServiceTest {

  @Mock private ProgressEntryRepository repository;
  @Mock private GradebookMembershipRepository membershipRepository;
  @Mock private GradebookMapper mapper;
  @InjectMocks private GradebookServiceImpl service;

  @Test
  @DisplayName("recordSubmissionGrade inserts a new entry with the score")
  void recordSubmissionGrade_new() {
    UUID submissionId = UUID.randomUUID();
    SubmissionGraded event =
        new SubmissionGraded(submissionId, UUID.randomUUID(), UUID.randomUUID(), 80);
    ProgressEntry fresh = new ProgressEntry();
    when(repository.findBySourceAndSourceId(GradeSource.HOMEWORK, submissionId))
        .thenReturn(Optional.empty());
    when(mapper.toEntry(event)).thenReturn(fresh);

    service.recordSubmissionGrade(event);

    ArgumentCaptor<ProgressEntry> saved = ArgumentCaptor.forClass(ProgressEntry.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue()).isSameAs(fresh);
    assertThat(saved.getValue().getScore()).isEqualTo(80);
  }

  @Test
  @DisplayName("recordSubmissionGrade updates the existing entry's score on a re-grade")
  void recordSubmissionGrade_regrade() {
    UUID submissionId = UUID.randomUUID();
    SubmissionGraded event =
        new SubmissionGraded(submissionId, UUID.randomUUID(), UUID.randomUUID(), 95);
    ProgressEntry existing = ProgressEntry.builder().score(50).build();
    when(repository.findBySourceAndSourceId(GradeSource.HOMEWORK, submissionId))
        .thenReturn(Optional.of(existing));
    when(mapper.toEntry(event)).thenReturn(new ProgressEntry());

    service.recordSubmissionGrade(event);

    assertThat(existing.getScore()).isEqualTo(95);
    verify(repository).save(existing);
  }

  @Test
  @DisplayName("recordEnrollment skips when the membership already exists")
  void recordEnrollment_idempotent() {
    StudentEnrolled event = new StudentEnrolled(UUID.randomUUID(), UUID.randomUUID());
    when(membershipRepository.existsByGroupIdAndStudentId(event.groupId(), event.userId()))
        .thenReturn(true);

    service.recordEnrollment(event);

    verify(membershipRepository, never()).save(any());
  }

  @Test
  @DisplayName("getStudentGradebook maps the student's entries")
  void getStudentGradebook_maps() {
    UUID studentId = UUID.randomUUID();
    List<ProgressEntry> entries = List.of(new ProgressEntry());
    StudentGradebookResponse dto = mock(StudentGradebookResponse.class);
    when(repository.findByStudentId(eq(studentId), any())).thenReturn(entries);
    when(mapper.toEntryResponses(entries)).thenReturn(List.of());
    when(mapper.toStudentGradebook(eq(studentId), any())).thenReturn(dto);

    assertThat(service.getStudentGradebook(studentId)).isSameAs(dto);
  }
}
