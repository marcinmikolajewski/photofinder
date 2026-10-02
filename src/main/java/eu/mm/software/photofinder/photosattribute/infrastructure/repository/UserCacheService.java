package eu.mm.software.photofinder.photosattribute.infrastructure.repository;

import eu.mm.software.photofinder.user.domain.User;
import eu.mm.software.photofinder.user.infrastructure.mongodb.UserSpringDataRepository;
import eu.mm.software.photofinder.user.domain.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCacheService {

    private final UserSpringDataRepository userSpringDataRepository; //fixme

    @Cacheable(value = "users", key = "#userId")
    public User getUser(String userId) {
        log.info("Read from db.");
        return userSpringDataRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    @Cacheable(value = "userIdByEmail", key = "#email")
    public String findUserIdByEmail(String email) {
        return Optional.ofNullable(userSpringDataRepository.findByUserEmail(email))
                .map(User::getId)
                .orElseThrow(UserNotFoundException::new);
    }
}
