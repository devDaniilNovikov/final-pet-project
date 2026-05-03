package dn.notificationservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class IdempotencyException extends RuntimeException{

    private final HttpStatus httpStatus;

    public IdempotencyException(String message,HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;

    }
}
