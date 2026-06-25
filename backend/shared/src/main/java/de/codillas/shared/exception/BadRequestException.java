package de.codillas.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** HTTP 400 — the request is invalid. Rendered as an RFC 9457 ProblemDetail by Spring. */
public class BadRequestException extends ErrorResponseException {

  public BadRequestException(String message) {
    super(
        HttpStatus.BAD_REQUEST,
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message),
        null);
  }
}
