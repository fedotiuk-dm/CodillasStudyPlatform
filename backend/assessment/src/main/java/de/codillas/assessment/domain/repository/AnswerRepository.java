package de.codillas.assessment.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.assessment.domain.model.Answer;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, UUID> {

  List<Answer> findByAttemptId(UUID attemptId);

  Optional<Answer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);
}
