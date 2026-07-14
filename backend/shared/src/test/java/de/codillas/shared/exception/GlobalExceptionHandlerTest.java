package de.codillas.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GlobalExceptionHandler — concurrency failures")
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("maps an optimistic-lock failure to 409 Conflict")
  void optimisticLockIsConflict() {
    ProblemDetail pd =
        handler.handleOptimisticLock(
            new ObjectOptimisticLockingFailureException("attempts", UUID.randomUUID()));
    assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
  }
}
