package de.codillas.homework.service;

import java.util.UUID;

import de.codillas.homework.api.dto.CreateRubricRequest;
import de.codillas.homework.api.dto.RubricResponse;

/** Authoring and reading of an assignment's optional grading rubric. */
public interface RubricService {

  RubricResponse createRubric(UUID assignmentId, CreateRubricRequest request);

  RubricResponse getRubric(UUID assignmentId);
}
