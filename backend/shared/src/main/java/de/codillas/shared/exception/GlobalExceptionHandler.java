package de.codillas.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Global error handling as RFC 9457 ProblemDetail. Spring already renders {@link
 * org.springframework.web.ErrorResponseException} subclasses (NotFound / Conflict / BadRequest) and
 * bean-validation failures; this advice adds method-security denials and a safe catch-all.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(AuthorizationDeniedException.class)
  ProblemDetail handleAccessDenied(AuthorizationDeniedException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
  }
}
