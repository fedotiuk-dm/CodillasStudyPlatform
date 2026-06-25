package de.codillas.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * HTTP 404 — a requested resource does not exist. Rendered as an RFC 9457 ProblemDetail by Spring.
 */
public class NotFoundException extends ErrorResponseException {

  public NotFoundException(String message) {
    super(
        HttpStatus.NOT_FOUND,
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, message),
        null);
  }

  public NotFoundException(String entityType, Object id) {
    this(entityType + " with id '" + id + "' not found");
  }
}
