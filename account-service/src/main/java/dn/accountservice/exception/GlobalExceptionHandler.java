package dn.accountservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {



    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<?> handleAccountNotFoundException(AccountNotFoundException e,
                                                            WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorBody.builder()
                        .code(HttpStatus.NOT_FOUND.value())
                        .path(request.getContextPath())
                        .message(e.getMessage())
                        .description(request.getDescription(false))
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
                                                                   WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorBody.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .path(request.getContextPath())
                        .message(e.getMessage())
                        .description(request.getDescription(false))
                        .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e,
                                                            WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorBody.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .path(request.getContextPath())
                        .message(e.getMessage())
                        .description(request.getDescription(false))
                        .build());
    }
}
