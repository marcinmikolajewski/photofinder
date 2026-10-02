package eu.mm.software.photofinder.photoparams.interfaces.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class DescribeFormValidatorTest {

    private DescribeFormValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DescribeFormValidator();
    }

    @Test
    void supportsDescribeFormDto() {
        assertThat(validator.supports(DescribeFormDto.class)).isTrue();
        assertThat(validator.supports(String.class)).isFalse();
    }

    @Test
    void validatesEmptyFields() {
        DescribeFormDto dto = new DescribeFormDto();
        Errors errors = new BeanPropertyBindingResult(dto, "describeFormDto");

        validator.validate(dto, errors);

        assertThat(errors.hasFieldErrors("provider")).isTrue();
        assertThat(errors.hasFieldErrors("path")).isTrue();
        assertThat(errors.hasFieldErrors("extensions")).isTrue();
    }

    @Test
    void validatesIncorrectProvider() {
        DescribeFormDto dto = new DescribeFormDto();
        dto.setProvider("NON_EXISTENT");
        dto.setPath("/tmp");
        dto.setExtensions(Set.of("jpg"));
        Errors errors = new BeanPropertyBindingResult(dto, "describeFormDto");

        validator.validate(dto, errors);

        assertThat(errors.hasFieldErrors("provider")).isTrue();
        assertThat(errors.getFieldError("provider").getCode()).isEqualTo("describeFormDto.provider.incorrect");
    }

    @Test
    void passesWithValidData_caseInsensitiveExtensions() {
        DescribeFormDto dto = new DescribeFormDto();
        dto.setProvider("OPENAI");
        dto.setPath("/photos");
        dto.setExtensions(Set.of("jpg", "NeF", "PNG"));
        Errors errors = new BeanPropertyBindingResult(dto, "describeFormDto");

        validator.validate(dto, errors);

        assertThat(errors.hasErrors()).isFalse();
    }

    @Test
    void failsWhenExtensionsContainUnsupportedOnes() {
        DescribeFormDto dto = new DescribeFormDto();
        dto.setProvider("OPENAI");
        dto.setPath("/photos");
        dto.setExtensions(Set.of("jpg", "gif"));
        Errors errors = new BeanPropertyBindingResult(dto, "describeFormDto");

        validator.validate(dto, errors);

        assertThat(errors.hasFieldErrors("extensions")).isTrue();
        assertThat(errors.getFieldError("extensions").getCode()).isEqualTo("describeFormDto.extensions.incorrect");
    }
}
