package eu.mm.software.photofinder.common.security;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "PhotoFinder API",
        version = "1.0.0",
        description = "API for managing photos and their attributes",
        contact = @Contact(name = "MM Software", email = "support@mm.software")
    ),
    security = @SecurityRequirement(name = "bearerAuth") // global security
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Bearer token for authorization"
)
public class OpenApiConfig {
}
