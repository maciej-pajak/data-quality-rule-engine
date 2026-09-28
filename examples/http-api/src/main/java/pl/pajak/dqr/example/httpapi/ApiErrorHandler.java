package pl.pajak.dqr.example.httpapi;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns what a caller can get wrong into a 400.
 *
 * <p>A batch over the cap and a record without an {@code id} are both rejected before the run
 * starts, so the whole request either answers a run or answers why there was none. A body that is
 * not a JSON array of objects never reaches the handler: Spring's own message converter rejects it.
 */
@RestControllerAdvice
class ApiErrorHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail onIllegalArgument(IllegalArgumentException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }
}
