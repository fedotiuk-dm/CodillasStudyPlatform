package de.codillas.shared.exception;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
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

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "The resource was modified concurrently; reload and retry");
  }

  // Overrides the base dispatcher's mapping for this type (a sibling @ExceptionHandler would be
  // ambiguous): renders a 413 ProblemDetail when an upload exceeds the multipart size limit.
  @Override
  protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ProblemDetail body =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file exceeds the size limit");
    return handleExceptionInternal(ex, body, headers, HttpStatus.PAYLOAD_TOO_LARGE, request);
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
  }
}
