package dn.notificationservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotificationNotFoundException extends RuntimeException{

    private final HttpStatus httpStatus;

    public NotificationNotFoundException(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public NotificationNotFoundException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

}
