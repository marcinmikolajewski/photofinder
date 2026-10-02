package eu.mm.software.photofinder.user.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Not found")
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("user not found");
    }
}
