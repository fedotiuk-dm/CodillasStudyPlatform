package de.codillas.gradebook.service;

import java.util.UUID;

import de.codillas.gradebook.api.dto.GroupGradebookResponse;
import de.codillas.gradebook.api.dto.StudentGradebookResponse;
import de.codillas.shared.event.AttemptCompleted;
import de.codillas.shared.event.GroupDeleted;
import de.codillas.shared.event.StudentEnrolled;
import de.codillas.shared.event.SubmissionGraded;

public interface GradebookService {

  void recordSubmissionGrade(SubmissionGraded event);

  void recordAttempt(AttemptCompleted event);

  void recordEnrollment(StudentEnrolled event);

  void purgeGroup(GroupDeleted event);

  StudentGradebookResponse getStudentGradebook(UUID studentId);

  GroupGradebookResponse getGroupGradebook(UUID groupId);
}
