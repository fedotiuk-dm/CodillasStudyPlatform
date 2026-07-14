package de.codillas.integration.assessment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.assessment.domain.model.Attempt;
import de.codillas.assessment.domain.model.AttemptStatus;
import de.codillas.assessment.domain.repository.AttemptRepository;
import de.codillas.integration.BaseIntegrationTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Transactional
@DisplayName("Attempt optimistic locking (integration)")
class AttemptOptimisticLockIntegrationTest extends BaseIntegrationTest {

  @Autowired AttemptRepository repository;
  @Autowired EntityManager em;

  @Test
  @DisplayName("a stale write over a newer version raises an optimistic-lock failure")
  void staleWriteIsRejected() {
    UUID id =
        repository
            .saveAndFlush(
                Attempt.builder()
                    .testId(UUID.randomUUID())
                    .studentId(UUID.randomUUID())
                    .status(AttemptStatus.IN_PROGRESS)
                    .build())
            .getId();
    em.clear();

    Attempt stale = repository.findById(id).orElseThrow(); // lock_version 0
    em.detach(stale);
    Attempt fresh = repository.findById(id).orElseThrow(); // lock_version 0
    fresh.setScore(50);
    repository.saveAndFlush(fresh); // -> lock_version 1

    stale.setScore(99);
    assertThatThrownBy(() -> repository.saveAndFlush(stale))
        .isInstanceOf(ObjectOptimisticLockingFailureException.class);
  }
}
