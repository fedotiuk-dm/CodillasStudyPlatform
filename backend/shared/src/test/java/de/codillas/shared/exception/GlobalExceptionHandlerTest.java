package de.codillas.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GlobalExceptionHandler — concurrency & upload failures")
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("maps an optimistic-lock failure to 409 Conflict")
  void optimisticLockIsConflict() {
    ProblemDetail pd =
        handler.handleOptimisticLock(
            new ObjectOptimisticLockingFailureException("attempts", java.util.UUID.randomUUID()));
    assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
  }

  @Test
  @DisplayName("maps an oversize upload to 413 Payload Too Large")
  void maxUploadSizeIsPayloadTooLarge() {
    ResponseEntity<Object> response =
        handler.handleMaxUploadSizeExceededException(
            new MaxUploadSizeExceededException(10),
            new HttpHeaders(),
            HttpStatus.PAYLOAD_TOO_LARGE,
            new ServletWebRequest(new MockHttpServletRequest()));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
    assertThat(((ProblemDetail) response.getBody()).getStatus())
        .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
  }
}
