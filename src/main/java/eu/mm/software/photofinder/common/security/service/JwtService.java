package eu.mm.software.photofinder.common.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    public static final String TOKEN_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    @Value("${jwt.secret.key}")
    private String secretKey;

    // Krótki access token (15 min). Front transparentnie odświeża na 401 przez refresh token.
    @Value("${application.security.jwt.expiration:900000}")
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-token.expiration:604800000}")
    private long refreshExpiration;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret key is not configured! Set JWT_SECRET_KEY environment variable.");
        }

        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);

            if (keyBytes.length < 32) {
                throw new IllegalStateException(
                        "JWT secret key is too short! Minimum 256 bits (32 bytes) required for HS256. " +
                        "Current key is " + (keyBytes.length * 8) + " bits. " +
                        "Generate a new key with: openssl rand -base64 64");
            }

            signingKey = Keys.hmacShaKeyFor(keyBytes);
            log.info("JWT service initialized successfully with {} bit key", keyBytes.length * 8);

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "JWT secret key is not valid Base64! " +
                    "Generate a new key with: openssl rand -base64 64", e);
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Zwraca typ tokenu ("access"/"refresh") lub null dla starszych tokenów bez claimu. */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get(TOKEN_TYPE, String.class));
    }

    /** Identyfikator tokenu (jti) — używany do rotacji/wykrywania ponownego użycia refresh tokenu. */
    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(typeClaim(TYPE_ACCESS), userDetails);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        // Unikalne jti pozwala rotować refresh token i wykrywać ponowne użycie starego.
        return buildToken(typeClaim(TYPE_REFRESH), userDetails, refreshExpiration, UUID.randomUUID().toString());
    }

    private static Map<String, Object> typeClaim(String type) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE, type);
        return claims;
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return buildToken(extraClaims, userDetails, expiration, null);
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration,
            String jti
    ) {
        long now = System.currentTimeMillis();
        var builder = Jwts.builder()
                .claims().add(extraClaims).and()
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(signingKey, Jwts.SIG.HS256);

        if (jti != null) {
            builder.id(jti);
        }
        return builder.compact();
    }
}
