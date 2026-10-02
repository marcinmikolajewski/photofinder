package eu.mm.software.photofinder.common.security.service;

import eu.mm.software.photofinder.photosattribute.infrastructure.repository.UserCacheService;
import eu.mm.software.photofinder.user.domain.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Optional;

/**
 * Rozwiązuje tożsamość aktualnie zalogowanego użytkownika na podstawie kontekstu bezpieczeństwa.
 *
 * <p>To przekrojowy aspekt uwierzytelniania (nie zapytanie CQRS, nie logika domenowa),
 * dlatego mieszka w {@code common.security} — warstwie spinającej Spring Security,
 * z której mogą korzystać kontrolery (interfaces) bez łamania izolacji warstw DDD.</p>
 *
 * <p>Subject tokenu JWT to email użytkownika, więc serwis tłumaczy email → userId.</p>
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserCacheService userCacheService;

    /**
     * @return id zalogowanego użytkownika
     * @throws UserNotFoundException jeśli brak uwierzytelnienia lub użytkownika w bazie
     */
    public String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = Optional.ofNullable(auth)
                .map(Principal::getName)
                .orElseThrow(UserNotFoundException::new);

        return userCacheService.findUserIdByEmail(email);
    }
}
