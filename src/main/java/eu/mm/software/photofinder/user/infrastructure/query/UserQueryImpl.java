package eu.mm.software.photofinder.user.infrastructure.query;

import eu.mm.software.photofinder.photosattribute.infrastructure.repository.UserCacheService;
import eu.mm.software.photofinder.user.application.query.UserDto;
import eu.mm.software.photofinder.user.application.query.UserQuery;
import eu.mm.software.photofinder.user.domain.User;
import eu.mm.software.photofinder.user.infrastructure.mongodb.UserSpringDataRepository;
import eu.mm.software.photofinder.user.domain.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
class UserQueryImpl implements UserQuery {

    private final UserSpringDataRepository userSpringDataRepository;
    private final UserCacheService userCacheService;

    @Override
    public List<UserDto> findAll() {

        return userSpringDataRepository.findAll().stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public UserDto findById(String userId) {
        return userSpringDataRepository.findById(userId)
                .map(this::convertToDto)
                .orElseThrow(UserNotFoundException::new);
    }

    @Override
    public String findUserNameByUserId(String userId) {
        return userSpringDataRepository.findById(userId)
                .map(User::getUserEmail)
                .orElseThrow(IllegalStateException::new);
    }

    @Override
    public String findLoggedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = Optional.ofNullable(auth)
                .map(Principal::getName)
                .orElseThrow(UserNotFoundException::new);

        return userCacheService.findUserIdByEmail(email);
    }

    private UserDto convertToDto(User user) {

        Assert.notNull(user, "user must not be null");

        return new UserDto(user.getId(),
                user.getUserName(),
                user.getUserEmail(),
                user.isActive(),
                user.getRoles().stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet())
        );
    }
}
