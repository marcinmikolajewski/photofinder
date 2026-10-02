package eu.mm.software.photofinder.common.security;

import eu.mm.software.photofinder.common.security.service.MongoUserDetailsService;
import eu.mm.software.photofinder.user.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableConfigurationProperties
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private static final String[] PUBLIC_URL = {
            "/rest/api/v1/authenticate",
            "/rest/api/v1/refresh-token",
            "/rest/api/v1/version",
            // Tylko health + prometheus publicznie (monitoring/scraping).
            // Pozostałe endpointy actuatora (env, beans, heapdump, mappings, loggers…)
            // wymagają ADMIN — patrz reguła poniżej.
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/prometheus"
    };

    // Dokumentacja API — publiczna tylko poza profilem prod (patrz filterChain).
    private static final String[] SWAGGER_URL = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final MongoUserDetailsService mongoUserDetailsService;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, Environment environment) throws Exception {

        boolean isProd = environment.acceptsProfiles(Profiles.of("prod"));

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> {
                    authz.requestMatchers(PUBLIC_URL).permitAll();

                    // Swagger/OpenAPI: publiczne tylko poza prod; w prod całkowicie blokowane.
                    if (isProd) {
                        authz.requestMatchers(SWAGGER_URL).denyAll();
                    } else {
                        authz.requestMatchers(SWAGGER_URL).permitAll();
                    }

                    authz.requestMatchers("/actuator/**").hasAuthority(Role.ADMIN.name())
                            .requestMatchers("/rest/api/v1/user/**").hasAuthority(Role.ADMIN.name())
                            .requestMatchers("/rest/api/v1/file/**").hasAnyAuthority(Role.ADMIN.name(), Role.CLIENT.name())
                            .requestMatchers("/rest/api/v1/photos/**").hasAnyAuthority(Role.ADMIN.name(), Role.CLIENT.name())
                            .anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(mongoUserDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());

        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}