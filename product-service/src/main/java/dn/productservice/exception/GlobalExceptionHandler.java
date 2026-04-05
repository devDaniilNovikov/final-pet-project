package dn.productservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorBody> handleProductNotFoundException(ProductNotFoundException e,
                                                                    WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorBody.builder()
                        .code(HttpStatus.NOT_FOUND.value())
                        .path(request.getDescription(false))
                        .message(e.getMessage())
                        .build());
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorBody> handleCategoryNotFoundException(CategoryNotFoundException e,
                                                                     WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorBody.builder()
                        .code(HttpStatus.NOT_FOUND.value())
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
