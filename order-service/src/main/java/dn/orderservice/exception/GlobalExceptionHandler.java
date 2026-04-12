package dn.orderservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorBody> handleProductNotFoundException(OrderNotFoundException e,
                                                                    WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorBody.builder()
                        .code(HttpStatus.NOT_FOUND.value())
                        .path(request.getDescription(false))
                        .message(e.getMessage())
                        .build());
    }



    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<ErrorBody> handleIllegalArgumentException(HttpStatusCodeException e,
                                                                    WebRequest request) {
        return ResponseEntity.status(e.getStatusCode())
                .body(ErrorBody.builder()
                        .code(e.getStatusCode().value())
                        .path(request.getDescription(false))
                        .message(e.getMessage())
                        .build());
    }



    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
                                                                           WebRequest request) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorBody.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .path(request.getDescription(false))
                        .message(message)
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleException(Exception e, WebRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorBody.builder()
                        .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .path(request.getDescription(false))
                        .message(e.getMessage())
                        .build());
    }
}
