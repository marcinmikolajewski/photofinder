package eu.mm.software.photofinder.user.interfaces.rest;

import eu.mm.software.photofinder.user.application.command.UserApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Objects;


@Component
public class UserValidator implements Validator {

    private final UserApplicationService userService;

    @Autowired
    public UserValidator(UserApplicationService userService) {
        this.userService = userService;
    }

    @Override
    public boolean supports(Class<?> clazz) {

        return UserEditForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        UserEditForm userEditForm = (UserEditForm) target;

        if (fieldIsNotEqual(userEditForm.getPasswordForm1(), userEditForm.getPasswordForm2())) {
            errors.rejectValue("passwordForm1", "userForm.password.notequal");
        }

		if (userService.isUserExist(userEditForm.getUserName())) {
			errors.rejectValue("userName", "userForm.username.notunique");
		}
    }

    private boolean fieldIsNotEqual(String passwordForm1, String passwordForm2) {

        return !Objects.equals(passwordForm1,passwordForm2);
    }
}
