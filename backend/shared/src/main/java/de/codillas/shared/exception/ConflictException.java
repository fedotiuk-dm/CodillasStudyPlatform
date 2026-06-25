package de.codillas.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * HTTP 409 — the request conflicts with current state. Rendered as an RFC 9457 ProblemDetail by
 * Spring.
 */
public class ConflictException extends ErrorResponseException {

  public ConflictException(String message) {
    super(
        HttpStatus.CONFLICT, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, message), null);
  }
}
