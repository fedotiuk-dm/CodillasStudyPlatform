package de.codillas.assessment.service;

import java.util.UUID;

import de.codillas.assessment.api.dto.AnswerResponse;
import de.codillas.assessment.api.dto.AttemptResponse;
import de.codillas.assessment.api.dto.GradeAnswerRequest;
import de.codillas.assessment.api.dto.SaveAnswerRequest;

public interface AttemptService {

  AttemptResponse startAttempt(UUID testId);

  AnswerResponse saveAnswer(UUID attemptId, SaveAnswerRequest request);

  AttemptResponse submitAttempt(UUID attemptId);

  AnswerResponse gradeAnswer(UUID attemptId, UUID answerId, GradeAnswerRequest request);

  AttemptResponse getAttempt(UUID attemptId);
}
