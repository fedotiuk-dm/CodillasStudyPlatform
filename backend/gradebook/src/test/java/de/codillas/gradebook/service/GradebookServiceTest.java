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

import de.codillas.gradebook.api.dto.CourseGradeResponse;
import de.codillas.gradebook.api.dto.StudentGradebookResponse;
import de.codillas.gradebook.domain.WeightedGrade;
import de.codillas.gradebook.domain.WeightedGradeCalculator;
import de.codillas.gradebook.domain.model.GradeSource;
import de.codillas.gradebook.domain.model.GradebookMembership;
import de.codillas.gradebook.domain.model.ProgressEntry;
import de.codillas.gradebook.domain.repository.GradebookMembershipRepository;
import de.codillas.gradebook.domain.repository.ProgressEntryRepository;
import de.codillas.gradebook.mapper.GradebookMapper;
import de.codillas.shared.event.AttemptCompleted;
import de.codillas.shared.event.StudentEnrolled;
import de.codillas.shared.event.SubmissionGraded;
import de.codillas.shared.security.CurrentUser;

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
  @Mock private WeightedGradeCalculator weightedGradeCalculator;
  @Mock private CurrentUser currentUser;
  @InjectMocks private GradebookServiceImpl service;

  @Test
  @DisplayName("recordSubmissionGrade inserts a new entry with the score")
  void recordSubmissionGrade_new() {
    UUID submissionId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    SubmissionGraded event =
        new SubmissionGraded(submissionId, UUID.randomUUID(), UUID.randomUUID(), 80, 100, groupId);
    ProgressEntry fresh = ProgressEntry.builder().score(80).maxPoints(100).groupId(groupId).build();
    when(repository.findBySourceAndSourceId(GradeSource.HOMEWORK, submissionId))
        .thenReturn(Optional.empty());
    when(mapper.toEntry(event)).thenReturn(fresh);

    service.recordSubmissionGrade(event);

    ArgumentCaptor<ProgressEntry> saved = ArgumentCaptor.forClass(ProgressEntry.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue()).isSameAs(fresh);
    assertThat(saved.getValue().getScore()).isEqualTo(80);
    assertThat(saved.getValue().getMaxPoints()).isEqualTo(100);
    assertThat(saved.getValue().getGroupId()).isEqualTo(groupId);
  }

  @Test
  @DisplayName("recordSubmissionGrade updates the existing entry's score on a re-grade")
  void recordSubmissionGrade_regrade() {
    UUID submissionId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    SubmissionGraded event =
        new SubmissionGraded(submissionId, UUID.randomUUID(), UUID.randomUUID(), 95, 100, groupId);
    ProgressEntry existing = ProgressEntry.builder().score(50).build();
    when(repository.findBySourceAndSourceId(GradeSource.HOMEWORK, submissionId))
        .thenReturn(Optional.of(existing));
    when(mapper.toEntry(event))
        .thenReturn(ProgressEntry.builder().score(95).maxPoints(100).groupId(groupId).build());

    service.recordSubmissionGrade(event);

    assertThat(existing.getScore()).isEqualTo(95);
    verify(repository).save(existing);
  }

  @Test
  @DisplayName("recordAttempt inserts the first attempt's score")
  void recordAttempt_firstAttempt() {
    UUID testId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    AttemptCompleted event =
        new AttemptCompleted(UUID.randomUUID(), testId, studentId, 60, 80, null);
    ProgressEntry fresh =
        ProgressEntry.builder()
            .studentId(studentId)
            .score(60)
            .maxPoints(80)
            .groupId(groupId)
            .build();
    when(repository.findBySourceAndReferenceIdAndStudentId(GradeSource.TEST, testId, studentId))
        .thenReturn(Optional.empty());
    when(mapper.toEntry(event)).thenReturn(fresh);

    service.recordAttempt(event);

    verify(repository).save(fresh);
    assertThat(fresh.getScore()).isEqualTo(60);
    assertThat(fresh.getMaxPoints()).isEqualTo(80);
  }

  @Test
  @DisplayName("recordAttempt raises the recorded score when a later attempt scores higher")
  void recordAttempt_betterAttempt_updates() {
    UUID testId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    UUID betterAttemptId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    AttemptCompleted event =
        new AttemptCompleted(betterAttemptId, testId, studentId, 90, 100, null);
    ProgressEntry existing = ProgressEntry.builder().id(UUID.randomUUID()).score(60).build();
    when(repository.findBySourceAndReferenceIdAndStudentId(GradeSource.TEST, testId, studentId))
        .thenReturn(Optional.of(existing));
    when(mapper.toEntry(event))
        .thenReturn(
            ProgressEntry.builder()
                .studentId(studentId)
                .score(90)
                .maxPoints(100)
                .groupId(groupId)
                .build());

    service.recordAttempt(event);

    assertThat(existing.getScore()).isEqualTo(90);
    assertThat(existing.getSourceId()).isEqualTo(betterAttemptId);
    verify(repository).save(existing);
  }

  @Test
  @DisplayName("recordAttempt keeps the best score when a later attempt scores lower")
  void recordAttempt_worseAttempt_keepsBest() {
    UUID testId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    AttemptCompleted event =
        new AttemptCompleted(UUID.randomUUID(), testId, studentId, 40, 100, null);
    ProgressEntry existing = ProgressEntry.builder().id(UUID.randomUUID()).score(80).build();
    when(repository.findBySourceAndReferenceIdAndStudentId(GradeSource.TEST, testId, studentId))
        .thenReturn(Optional.of(existing));
    when(mapper.toEntry(event))
        .thenReturn(
            ProgressEntry.builder()
                .studentId(studentId)
                .score(40)
                .maxPoints(100)
                .groupId(groupId)
                .build());

    service.recordAttempt(event);

    assertThat(existing.getScore()).isEqualTo(80);
    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("recordAttempt resolves a null event group from the student's membership")
  void recordAttempt_nullGroup_resolvedFromMembership() {
    UUID testId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    UUID attemptId = UUID.randomUUID();
    AttemptCompleted event = new AttemptCompleted(attemptId, testId, studentId, 70, 100, null);
    ProgressEntry fresh =
        ProgressEntry.builder().studentId(studentId).score(70).maxPoints(100).build();
    when(repository.findBySourceAndReferenceIdAndStudentId(GradeSource.TEST, testId, studentId))
        .thenReturn(Optional.empty());
    when(mapper.toEntry(event)).thenReturn(fresh);
    when(membershipRepository.findFirstByStudentId(studentId))
        .thenReturn(
            Optional.of(
                GradebookMembership.builder().studentId(studentId).groupId(groupId).build()));

    service.recordAttempt(event);

    ArgumentCaptor<ProgressEntry> saved = ArgumentCaptor.forClass(ProgressEntry.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue().getGroupId()).isEqualTo(groupId);
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
  @DisplayName("getStudentGradebook maps the student's entries and attaches the course grade")
  void getStudentGradebook_maps() {
    UUID studentId = UUID.randomUUID();
    List<ProgressEntry> entries = List.of(new ProgressEntry());
    StudentGradebookResponse dto = mock(StudentGradebookResponse.class);
    CourseGradeResponse courseGrade = mock(CourseGradeResponse.class);
    WeightedGrade weighted = new WeightedGrade(0, 0, List.of());
    when(currentUser.isStaff()).thenReturn(true);
    when(repository.findByStudentId(eq(studentId), any())).thenReturn(entries);
    when(mapper.toEntryResponses(entries)).thenReturn(List.of());
    when(weightedGradeCalculator.compute(entries)).thenReturn(weighted);
    when(mapper.toCourseGrade(weighted)).thenReturn(courseGrade);
    when(mapper.toStudentGradebook(eq(studentId), any(), eq(courseGrade))).thenReturn(dto);

    assertThat(service.getStudentGradebook(studentId)).isSameAs(dto);
  }

  @Test
  @DisplayName("getStudentGradebook delegates the weighted course grade to the calculator")
  void getStudentGradebook_computesCourseGrade() {
    UUID studentId = UUID.randomUUID();
    List<ProgressEntry> entries =
        List.of(
            ProgressEntry.builder().source(GradeSource.HOMEWORK).score(34).maxPoints(40).build(),
            ProgressEntry.builder().source(GradeSource.TEST).score(18).maxPoints(20).build());
    StudentGradebookResponse dto = mock(StudentGradebookResponse.class);
    CourseGradeResponse courseGrade = mock(CourseGradeResponse.class);
    WeightedGrade weighted = new WeightedGrade(52, 60, List.of());
    when(currentUser.isStaff()).thenReturn(true);
    when(repository.findByStudentId(eq(studentId), any())).thenReturn(entries);
    when(mapper.toEntryResponses(entries)).thenReturn(List.of());
    when(weightedGradeCalculator.compute(entries)).thenReturn(weighted);
    when(mapper.toCourseGrade(weighted)).thenReturn(courseGrade);
    when(mapper.toStudentGradebook(eq(studentId), any(), eq(courseGrade))).thenReturn(dto);

    assertThat(service.getStudentGradebook(studentId)).isSameAs(dto);
    verify(weightedGradeCalculator).compute(entries);
    verify(mapper).toCourseGrade(weighted);
  }

  @Test
  @DisplayName("a student reading another student's gradebook gets 404")
  void getStudentGradebook_otherStudent_notFound() {
    UUID owner = UUID.randomUUID();
    when(currentUser.isStaff()).thenReturn(false);
    when(currentUser.id()).thenReturn(UUID.randomUUID());

    org.assertj.core.api.Assertions.assertThatExceptionOfType(
            de.codillas.shared.exception.NotFoundException.class)
        .isThrownBy(() -> service.getStudentGradebook(owner));
  }

  @Test
  @DisplayName("a non-member reading a group gradebook gets 404")
  void getGroupGradebook_nonMember_notFound() {
    UUID groupId = UUID.randomUUID();
    UUID caller = UUID.randomUUID();
    when(currentUser.isStaff()).thenReturn(false);
    when(currentUser.id()).thenReturn(caller);
    when(membershipRepository.existsByGroupIdAndStudentId(groupId, caller)).thenReturn(false);

    org.assertj.core.api.Assertions.assertThatExceptionOfType(
            de.codillas.shared.exception.NotFoundException.class)
        .isThrownBy(() -> service.getGroupGradebook(groupId));
  }
}
