package eu.mm.software.photofinder.photoparams.interfaces.rest;

import eu.mm.software.photofinder.photosattribute.domain.AiProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Arrays;
import java.util.List;

@Component
public class DescribeFormValidator implements Validator {

    private static final List<String> CORRECT_EXTENSIONS = List.of("NEF", "JPG", "DNG", "CR2", "PNG", "ARW", "PEF");

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.isAssignableFrom(DescribeFormDto.class);
    }

    @Override
    public void validate(Object target, Errors errors) { //fixme add controller advise i obsługę błędów oraz tłumaczenia

        DescribeFormDto describeFormDto = (DescribeFormDto) target;

        if (StringUtils.isEmpty(describeFormDto.getProvider())) {
            errors.rejectValue("provider", "describeFormDto.provider.empty");
        } else {
            if (providerNonMatchToAllProvider(describeFormDto)) {
                errors.rejectValue("provider", "describeFormDto.provider.incorrect");
            }
        }

        if (StringUtils.isEmpty(describeFormDto.getPath())) {
            errors.rejectValue("path", "describeFormDto.path.empty");
        }

        if (describeFormDto.getExtensions().isEmpty()) {
            errors.rejectValue("extensions", "describeFormDto.extensions.empty");
        } else {
            if (!describeFormDto.getExtensions().stream()
                    .allMatch(it -> CORRECT_EXTENSIONS.contains(it.toUpperCase()))) {
                errors.rejectValue("extensions", "describeFormDto.extensions.incorrect");
            }
        }
    }

    private static boolean providerNonMatchToAllProvider(DescribeFormDto describeFormDto) {
        return Arrays.stream(AiProvider.values())
                .noneMatch(it -> it.name().equals(describeFormDto.getProvider()));
    }
}
