package eu.mm.software.photofinder.common.security;

import eu.mm.software.photofinder.common.security.dto.AuthenticationResponse;
import eu.mm.software.photofinder.common.security.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RequestMapping("/rest/api/v1")
@RestController
@Tag(
    name = "Authentication",
    description = "Endpoints for user authentication and JWT token management"
)
public class AuthController {

    private final AuthenticationService service;
    private final LoginRateLimiter loginRateLimiter;

    @Autowired
    public AuthController(AuthenticationService service, LoginRateLimiter loginRateLimiter) {
        this.service = service;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping(value = "/authenticate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Authenticate user",
        description = "Authenticates a user with username and password and returns a JWT token."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentication successful, token returned"),
        @ApiResponse(responseCode = "400", description = "Invalid credentials or request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Parameter(description = "User authentication request containing username and password")
            @RequestBody AuthRequest request,
            HttpServletRequest httpRequest
    ) {
        if (!loginRateLimiter.tryAcquire(clientIp(httpRequest))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        return ResponseEntity.ok(service.authenticate(request));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/refresh-token")
    @Operation(
        summary = "Refresh JWT token",
        description = "Refreshes an existing JWT token. Requires a valid refresh token in the request."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized or invalid refresh token")
    })
    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        service.refreshToken(request, response);
    }
}
