package eu.mm.software.photofinder.common.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import eu.mm.software.photofinder.common.security.AuthRequest;
import eu.mm.software.photofinder.common.security.dto.AuthenticationResponse;
import eu.mm.software.photofinder.user.domain.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Service
public class AuthenticationService {

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final MongoUserDetailsService mongoUserDetailsService;
    private final RefreshTokenService refreshTokenService;

    @Autowired
    public AuthenticationService(JwtService jwtService,
                                 AuthenticationManager authenticationManager,
                                 MongoUserDetailsService mongoUserDetailsService,
                                 RefreshTokenService refreshTokenService) {
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.mongoUserDetailsService = mongoUserDetailsService;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthenticationResponse authenticate(AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUserEmail(),
                            request.getPassword()
                    )
            );

            UserDetails user = (UserDetails) authentication.getPrincipal();

            var jwtToken = jwtService.generateToken(user);
            var refreshToken = jwtService.generateRefreshToken(user);

            refreshTokenService.rotate(
                    user.getUsername(),
                    jwtService.extractJti(refreshToken)
            );

            log.info("User authenticated: {}", user.getUsername());

            return new AuthenticationResponse(jwtToken, refreshToken);

        } catch (AuthenticationException e) {
            log.warn("Failed login attempt for user: {}", request.getUserEmail());
            throw e;
        }
    }

    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final String refreshToken = authHeader.substring(7);
        try {
            // Tylko token typu REFRESH może odnowić sesję — access tokenem nie odświeżamy.
            if (!JwtService.TYPE_REFRESH.equals(jwtService.extractTokenType(refreshToken))) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            final String userEmail = jwtService.extractUsername(refreshToken);
            if (userEmail == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // Rotacja + wykrycie ponownego użycia: akceptuj tylko NAJNOWSZY refresh token.
            // Stary/skradziony (nieaktualne jti) → odrzuć i unieważnij całą sesję refresh.
            if (!refreshTokenService.isCurrent(userEmail, jwtService.extractJti(refreshToken))) {
                // Zdarzenie bezpieczeństwa: użyto starego/nieaktualnego refresh tokenu → unieważniamy sesję.
                log.warn("Refresh token reuse detected for user: {} — revoking refresh session", userEmail);
                refreshTokenService.revoke(userEmail);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            var user = Optional.ofNullable(mongoUserDetailsService.loadUserByUsername(userEmail))
                    .orElseThrow(UserNotFoundException::new);

            if (jwtService.validateToken(refreshToken, user)) {
                var accessToken = jwtService.generateToken(user);
                var newRefreshToken = jwtService.generateRefreshToken(user);
                refreshTokenService.rotate(userEmail, jwtService.extractJti(newRefreshToken));

                var authResponse = new AuthenticationResponse(accessToken, newRefreshToken);
                response.setContentType("application/json");
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
                log.info("Access token refreshed for user: {}", userEmail);
            } else {
                log.warn("Refresh rejected: invalid/expired token for user: {}", userEmail);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Refresh rejected: malformed token ({})", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}