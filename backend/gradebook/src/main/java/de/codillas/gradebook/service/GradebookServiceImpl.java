package de.codillas.gradebook.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.gradebook.api.dto.GroupGradebookResponse;
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
import de.codillas.shared.event.GroupDeleted;
import de.codillas.shared.event.StudentEnrolled;
import de.codillas.shared.event.SubmissionGraded;
import de.codillas.shared.exception.NotFoundException;
import de.codillas.shared.security.CurrentUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradebookServiceImpl implements GradebookService {

  private final ProgressEntryRepository repository;
  private final GradebookMembershipRepository membershipRepository;
  private final GradebookMapper mapper;
  private final WeightedGradeCalculator weightedGradeCalculator;
  private final CurrentUser currentUser;

  @Override
  @Transactional
  public void recordSubmissionGrade(SubmissionGraded event) {
    ProgressEntry fresh = mapper.toEntry(event);
    resolveGroup(fresh);
    ProgressEntry entry =
        repository
            .findBySourceAndSourceId(GradeSource.HOMEWORK, event.submissionId())
            .orElse(fresh);
    entry.setScore(fresh.getScore());
    entry.setMaxPoints(fresh.getMaxPoints());
    entry.setGroupId(fresh.getGroupId());
    repository.save(entry);
  }

  @Override
  @Transactional
  public void recordAttempt(AttemptCompleted event) {
    ProgressEntry fresh = mapper.toEntry(event);
    resolveGroup(fresh);
    ProgressEntry entry =
        repository
            .findBySourceAndReferenceIdAndStudentId(
                GradeSource.TEST, event.testId(), event.studentId())
            .orElse(fresh);
    if (entry.getId() == null || fresh.getScore() > entry.getScore()) {
      entry.setScore(fresh.getScore());
      entry.setMaxPoints(fresh.getMaxPoints());
      entry.setGroupId(fresh.getGroupId());
      entry.setSourceId(event.attemptId()); // point at the best attempt
      repository.save(entry);
    }
  }

  @Override
  @Transactional
  public void recordEnrollment(StudentEnrolled event) {
    if (!membershipRepository.existsByGroupIdAndStudentId(event.groupId(), event.userId())) {
      membershipRepository.save(mapper.toMembership(event));
    }
  }

  @Override
  @Transactional
  public void purgeGroup(GroupDeleted event) {
    membershipRepository.deleteByGroupId(event.groupId());
  }

  @Override
  public StudentGradebookResponse getStudentGradebook(UUID studentId) {
    if (!currentUser.isStaff() && !studentId.equals(currentUser.id())) {
      throw new NotFoundException("Gradebook", studentId);
    }
    return studentGradebook(
        studentId, repository.findByStudentId(studentId, ProgressEntryRepository.BY_RECORDED));
  }

  @Override
  public GroupGradebookResponse getGroupGradebook(UUID groupId) {
    if (!currentUser.isStaff()
        && !membershipRepository.existsByGroupIdAndStudentId(groupId, currentUser.id())) {
      throw new NotFoundException("Gradebook", groupId);
    }
    List<UUID> studentIds =
        membershipRepository.findByGroupId(groupId).stream()
            .map(GradebookMembership::getStudentId)
            .distinct()
            .toList();

    // Scope each member's course grade to this group so it is per-(student, group).
    List<StudentGradebookResponse> students =
        studentIds.stream()
            .map(
                id ->
                    studentGradebook(
                        id,
                        repository.findByStudentIdAndGroupId(
                            id, groupId, ProgressEntryRepository.BY_RECORDED)))
            .toList();
    return mapper.toGroupGradebook(groupId, students);
  }

  /** Map a student's entries and attach the points-weighted course grade. */
  private StudentGradebookResponse studentGradebook(UUID studentId, List<ProgressEntry> entries) {
    WeightedGrade weighted = weightedGradeCalculator.compute(entries);
    return mapper.toStudentGradebook(
        studentId, mapper.toEntryResponses(entries), mapper.toCourseGrade(weighted));
  }

  /**
   * Backfill a null event {@code groupId} from the student's own membership read model — assessment
   * cannot know the group (a test is lesson-scoped and reused across cohorts). A student in
   * multiple groups resolves to their first membership; multi-group precision is deferred.
   */
  private void resolveGroup(ProgressEntry fresh) {
    if (fresh.getGroupId() == null) {
      membershipRepository
          .findFirstByStudentId(fresh.getStudentId())
          .ifPresent(m -> fresh.setGroupId(m.getGroupId()));
    }
  }
}
