package eu.mm.software.photofinder.common.security;

import eu.mm.software.photofinder.common.security.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private UserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;

    private final UserDetails user = User.withUsername("user@example.com")
            .password("x")
            .authorities("CLIENT")
            .build();

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        String key = Base64.getEncoder()
                .encodeToString("0123456789012345678901234567890123456789012345".getBytes());
        ReflectionTestUtils.setField(jwtService, "secretKey", key);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3_600_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 86_400_000L);
        jwtService.init();

        userDetailsService = mock(UserDetailsService.class);
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(user);

        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void accessToken_authenticatesRequest() throws Exception {
        String token = jwtService.generateToken(user);
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        var chain = new MockFilterChain();

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("user@example.com");
        assertThat(chain.getRequest()).isNotNull(); // łańcuch kontynuowany
    }

    @Test
    void refreshToken_doesNotAuthenticate() throws Exception {
        String refresh = jwtService.generateRefreshToken(user);
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + refresh);
        var chain = new MockFilterChain();

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void malformedToken_doesNotThrow_andDoesNotAuthenticate() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer not-a-real-jwt");
        var chain = new MockFilterChain();

        // nie powinno rzucić wyjątku (brak 500) — łańcuch leci dalej
        filter.doFilter(req, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void noAuthorizationHeader_passesThrough() throws Exception {
        var req = new MockHttpServletRequest();
        var chain = new MockFilterChain();

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }
}
