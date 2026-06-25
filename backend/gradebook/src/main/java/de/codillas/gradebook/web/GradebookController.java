package de.codillas.gradebook.web;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import de.codillas.gradebook.api.GradebookApi;
import de.codillas.gradebook.api.dto.GroupGradebookResponse;
import de.codillas.gradebook.api.dto.StudentGradebookResponse;
import de.codillas.gradebook.service.GradebookService;
import de.codillas.shared.security.RequiresAuthenticated;

import lombok.RequiredArgsConstructor;

/** Thin delegator — implements the generated {@link GradebookApi}. */
@RestController
@RequiredArgsConstructor
public class GradebookController implements GradebookApi {

  private final GradebookService gradebookService;

  @Override
  @RequiresAuthenticated
  public ResponseEntity<StudentGradebookResponse> getStudentGradebook(UUID studentId) {
    return ResponseEntity.ok(gradebookService.getStudentGradebook(studentId));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<GroupGradebookResponse> getGroupGradebook(UUID groupId) {
    return ResponseEntity.ok(gradebookService.getGroupGradebook(groupId));
  }
}
