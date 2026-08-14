package app.mata.gradup.exception;

import app.mata.gradup.endpoint.rest.model.Error;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@AllArgsConstructor
public class ErrorHandler {

  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Error handleNotFound(NotFoundException e) {
    return new Error().code("NOT_FOUND").message(e.getMessage());
  }

  @ExceptionHandler(ConflictException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public Error handleConflict(ConflictException e) {
    return new Error().code("CONFLICT").message(e.getMessage());
  }

  @ExceptionHandler(BusinessRuleException.class)
  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  public Error handleUnprocessable(BusinessRuleException e) {
    return new Error().code("UNPROCESSABLE_ENTITY").message(e.getMessage());
  }

  @ExceptionHandler(BadRequestException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Error handleBadRequest(BadRequestException e) {
    return new Error().code("BAD_REQUEST").message(e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Error handleValidation(MethodArgumentNotValidException e) {
    return new Error().code("BAD_REQUEST").message(e.getMessage());
  }
}
