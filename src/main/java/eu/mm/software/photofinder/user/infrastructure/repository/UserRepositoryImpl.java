package eu.mm.software.photofinder.user.infrastructure.repository;

import eu.mm.software.photofinder.user.domain.UserFormAttributes;
import eu.mm.software.photofinder.user.domain.Role;
import eu.mm.software.photofinder.user.domain.User;
import eu.mm.software.photofinder.user.domain.UserNotFoundException;
import eu.mm.software.photofinder.user.domain.UserRepository;
import eu.mm.software.photofinder.user.infrastructure.mongodb.UserSpringDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
class UserRepositoryImpl implements UserRepository {

    private final UserSpringDataRepository userSpringDataRepository;


    @Override
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#userId"),
            @CacheEvict(value = "userIdByEmail", allEntries = true)})
    public void update(UserFormAttributes userFormAttributes, String userId) {

        User userToUpdate = userSpringDataRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        User userToSave = new User(userToUpdate.getId(),
                userFormAttributes.getUserName(),
                userFormAttributes.getUserEmail(),
                BCrypt.hashpw(userFormAttributes.getPasswordForm1(), BCrypt.gensalt()),
                userFormAttributes.getActive(),
                userFormAttributes.getRoles().stream()
                        .map(Role::valueOf)
                        .collect(Collectors.toUnmodifiableSet()));

        userSpringDataRepository.save(userToSave);
    }

    @Override
    public String save(UserFormAttributes userFormAttributes) {

        User user = new User(userFormAttributes.getId(),
                userFormAttributes.getUserName(),
                userFormAttributes.getUserEmail(),
                BCrypt.hashpw(userFormAttributes.getPasswordForm1(), BCrypt.gensalt()),
                userFormAttributes.getActive(),
                userFormAttributes.getRoles().stream()
                        .map(Role::valueOf)
                        .collect(Collectors.toUnmodifiableSet()));

        User saveEntity = userSpringDataRepository.save(user);

        return saveEntity.getId();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#id"),
            @CacheEvict(value = "userIdByEmail", allEntries = true)})
    public void delete(String id) {

        userSpringDataRepository.findById(id)
                .ifPresent(userSpringDataRepository::delete);
    }

    @Override
    public Optional<User> findByUserName(String name) {

        return Optional.ofNullable(userSpringDataRepository.findByUserName(name));
    }
}
