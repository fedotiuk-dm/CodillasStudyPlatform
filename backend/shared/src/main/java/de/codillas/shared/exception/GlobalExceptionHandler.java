package de.codillas.shared.exception;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Global error handling as RFC 9457 ProblemDetail. The base {@link ResponseEntityExceptionHandler}
 * already renders Spring MVC failures — bean validation, unreadable bodies, and {@code
 * ErrorResponse} types (our NotFound / Conflict / BadRequest, plus an oversize upload as 413). This
 * advice adds method-security denials, optimistic-lock conflicts, and a safe catch-all.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(AuthorizationDeniedException.class)
  ProblemDetail handleAccessDenied(AuthorizationDeniedException ex) {
    log.debug("Access denied: {}", ex.getMessage());
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
    log.warn("Optimistic lock conflict: {}", ex.getMessage());
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "The resource was modified concurrently; reload and retry");
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
  }
}
