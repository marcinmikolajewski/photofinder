package eu.mm.software.photofinder.common.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Rotacja refresh tokenów z wykrywaniem ponownego użycia.
 *
 * <p>Dla każdego użytkownika (subject = email) trzymamy <b>jedno</b> aktualne {@code jti}
 * refresh tokenu w kolekcji {@code refresh_tokens}. Przy każdym logowaniu/odświeżeniu
 * jti jest podmieniane ({@link #rotate}). Odświeżenie akceptowane jest tylko, gdy
 * przedstawione jti odpowiada zapisanemu ({@link #isCurrent}) — stary (skradziony/ponownie
 * użyty) refresh token zostaje odrzucony, mimo że kryptograficznie wciąż jest ważny.</p>
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String COLLECTION = "refresh_tokens";

    private final MongoTemplate mongoTemplate;

    /** Zapisuje (upsert) bieżące jti dla użytkownika. */
    public void rotate(String subject, String jti) {
        Query query = new Query(Criteria.where("_id").is(subject));
        Update update = new Update()
                .set("jti", jti)
                .set("updatedAt", Instant.now());
        mongoTemplate.upsert(query, update, COLLECTION);
    }

    /** True, jeśli podane jti jest aktualnym refresh-jti danego użytkownika. */
    public boolean isCurrent(String subject, String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        Query query = new Query(Criteria.where("_id").is(subject).and("jti").is(jti));
        return mongoTemplate.exists(query, COLLECTION);
    }

    /** Unieważnia refresh token użytkownika (np. przy wykryciu ponownego użycia). */
    public void revoke(String subject) {
        mongoTemplate.remove(new Query(Criteria.where("_id").is(subject)), COLLECTION);
    }
}
