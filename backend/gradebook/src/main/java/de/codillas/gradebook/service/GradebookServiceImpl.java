package de.codillas.gradebook.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.gradebook.api.dto.GroupGradebookResponse;
import de.codillas.gradebook.api.dto.StudentGradebookResponse;
import de.codillas.gradebook.domain.model.GradeSource;
import de.codillas.gradebook.domain.model.GradebookMembership;
import de.codillas.gradebook.domain.model.ProgressEntry;
import de.codillas.gradebook.domain.repository.GradebookMembershipRepository;
import de.codillas.gradebook.domain.repository.ProgressEntryRepository;
import de.codillas.gradebook.mapper.GradebookMapper;
import de.codillas.shared.event.AttemptCompleted;
import de.codillas.shared.event.StudentEnrolled;
import de.codillas.shared.event.SubmissionGraded;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradebookServiceImpl implements GradebookService {

  private final ProgressEntryRepository repository;
  private final GradebookMembershipRepository membershipRepository;
  private final GradebookMapper mapper;

  @Override
  @Transactional
  public void recordSubmissionGrade(SubmissionGraded event) {
    upsert(GradeSource.HOMEWORK, event.submissionId(), event.score(), mapper.toEntry(event));
  }

  @Override
  @Transactional
  public void recordAttempt(AttemptCompleted event) {
    upsert(GradeSource.TEST, event.attemptId(), event.score(), mapper.toEntry(event));
  }

  @Override
  @Transactional
  public void recordEnrollment(StudentEnrolled event) {
    if (!membershipRepository.existsByGroupIdAndStudentId(event.groupId(), event.userId())) {
      membershipRepository.save(mapper.toMembership(event));
    }
  }

  @Override
  public StudentGradebookResponse getStudentGradebook(UUID studentId) {
    return mapper.toStudentGradebook(
        studentId,
        mapper.toEntryResponses(
            repository.findByStudentId(studentId, ProgressEntryRepository.BY_RECORDED)));
  }

  @Override
  public GroupGradebookResponse getGroupGradebook(UUID groupId) {
    List<UUID> studentIds =
        membershipRepository.findByGroupId(groupId).stream()
            .map(GradebookMembership::getStudentId)
            .distinct()
            .toList();
    Map<UUID, List<ProgressEntry>> byStudent =
        studentIds.isEmpty()
            ? Map.of()
            : repository.findByStudentIdIn(studentIds, ProgressEntryRepository.BY_RECORDED).stream()
                .collect(Collectors.groupingBy(ProgressEntry::getStudentId));

    List<StudentGradebookResponse> students =
        studentIds.stream()
            .map(
                id ->
                    mapper.toStudentGradebook(
                        id, mapper.toEntryResponses(byStudent.getOrDefault(id, List.of()))))
            .toList();
    return mapper.toGroupGradebook(groupId, students);
  }

  /** Insert or, on a re-grade of the same artefact, update the score in place. */
  private void upsert(GradeSource source, UUID sourceId, int score, ProgressEntry fresh) {
    ProgressEntry entry = repository.findBySourceAndSourceId(source, sourceId).orElse(fresh);
    entry.setScore(score);
    repository.save(entry);
  }
}
