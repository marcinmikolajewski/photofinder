package eu.mm.software.photofinder.user.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Validation error")
public class UserValidationException extends Throwable {

    public UserValidationException(String message) {
        super(message);
    }
}
