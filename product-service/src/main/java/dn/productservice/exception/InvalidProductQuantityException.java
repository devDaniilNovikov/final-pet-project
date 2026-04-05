package dn.productservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidProductQuantityException extends RuntimeException {

    public InvalidProductQuantityException(String message) {
        super(message);
    }

    public InvalidProductQuantityException() {
    }
}
