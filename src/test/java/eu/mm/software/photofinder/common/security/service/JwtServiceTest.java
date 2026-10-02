package eu.mm.software.photofinder.common.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private final UserDetails user = User.withUsername("user@example.com")
            .password("x")
            .authorities("CLIENT")
            .build();

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // 48-bajtowy klucz (>= 256 bit) zakodowany Base64
        String key = Base64.getEncoder()
                .encodeToString("0123456789012345678901234567890123456789012345".getBytes());
        ReflectionTestUtils.setField(jwtService, "secretKey", key);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3_600_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 86_400_000L);
        jwtService.init();
    }

    @Test
    void accessToken_hasTypeAccess_andCarriesUsername() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractTokenType(token)).isEqualTo(JwtService.TYPE_ACCESS);
        assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
        assertThat(jwtService.validateToken(token, user)).isTrue();
    }

    @Test
    void refreshToken_hasTypeRefresh() {
        String token = jwtService.generateRefreshToken(user);

        assertThat(jwtService.extractTokenType(token)).isEqualTo(JwtService.TYPE_REFRESH);
        assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
    }

    @Test
    void accessAndRefreshTokens_areDistinguishable() {
        String access = jwtService.generateToken(user);
        String refresh = jwtService.generateRefreshToken(user);

        assertThat(jwtService.extractTokenType(access)).isNotEqualTo(jwtService.extractTokenType(refresh));
    }

    @Test
    void refreshToken_hasUniqueJti_accessTokenHasNone() {
        String access = jwtService.generateToken(user);
        String refresh1 = jwtService.generateRefreshToken(user);
        String refresh2 = jwtService.generateRefreshToken(user);

        assertThat(jwtService.extractJti(access)).isNull();
        assertThat(jwtService.extractJti(refresh1)).isNotBlank();
        assertThat(jwtService.extractJti(refresh1))
                .isNotEqualTo(jwtService.extractJti(refresh2)); // każdy refresh ma inne jti
    }
}
