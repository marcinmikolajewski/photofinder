package eu.mm.software.photofinder.user.interfaces.rest;

import eu.mm.software.photofinder.user.application.command.UserApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserValidatorTest {

    private UserApplicationService userApplicationService;
    private UserValidator validator;

    @BeforeEach
    void setUp() {
        userApplicationService = mock(UserApplicationService.class);
        validator = new UserValidator(userApplicationService);
    }

    @Test
    void supportsUserEditFormOnly() {
        assertThat(validator.supports(UserEditForm.class)).isTrue();
        assertThat(validator.supports(String.class)).isFalse();
    }

    @Test
    void rejectsWhenPasswordsDoNotMatch() {
        UserEditForm form = new UserEditForm(
                null,
                "john",
                "john@example.com",
                "pass1",
                "pass2",
                true,
                Set.of("USER")
        );
        when(userApplicationService.isUserExist("john")).thenReturn(false);

        Errors errors = new BeanPropertyBindingResult(form, "userEditForm");
        validator.validate(form, errors);

        assertThat(errors.hasFieldErrors("passwordForm1")).isTrue();
        assertThat(errors.getFieldError("passwordForm1").getCode()).isEqualTo("userForm.password.notequal");
        // ensure username uniqueness not triggered when service says user does not exist
        assertThat(errors.hasFieldErrors("userName")).isFalse();
    }

    @Test
    void rejectsWhenUsernameAlreadyExists() {
        UserEditForm form = new UserEditForm(
                null,
                "existingUser",
                "ex@example.com",
                "secret",
                "secret",
                true,
                Set.of("USER")
        );
        when(userApplicationService.isUserExist("existingUser")).thenReturn(true);

        Errors errors = new BeanPropertyBindingResult(form, "userEditForm");
        validator.validate(form, errors);

        assertThat(errors.hasFieldErrors("userName")).isTrue();
        assertThat(errors.getFieldError("userName").getCode()).isEqualTo("userForm.username.notunique");
        // passwords equal -> no password error
        assertThat(errors.hasFieldErrors("passwordForm1")).isFalse();
    }

    @Test
    void passesWhenValid() {
        UserEditForm form = new UserEditForm(
                null,
                "newuser",
                "new@example.com",
                "secret",
                "secret",
                true,
                Set.of("ADMIN")
        );
        when(userApplicationService.isUserExist("newuser")).thenReturn(false);

        Errors errors = new BeanPropertyBindingResult(form, "userEditForm");
        validator.validate(form, errors);

        assertThat(errors.hasErrors()).isFalse();
    }
}
