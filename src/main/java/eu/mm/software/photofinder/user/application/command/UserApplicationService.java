package eu.mm.software.photofinder.user.application.command;

import eu.mm.software.photofinder.user.domain.UserFormAttributes;

import eu.mm.software.photofinder.photosattribute.domain.event.DomainEventPublisher;
import eu.mm.software.photofinder.user.domain.UserRepository;
import eu.mm.software.photofinder.user.domain.event.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;
    private final DomainEventPublisher eventPublisher;


    public String save(UserFormAttributes userFormAttributes) {
        String id = userRepository.save(userFormAttributes);
        log.info("User created: id={}, userName={}", id, userFormAttributes.getUserName());
        return id;
    }

    public void delete(String id) {
        log.info("Deleting user: id={}", id);
        userRepository.delete(id);
        eventPublisher.publish(new UserDeletedEvent(id));
    }

    public void update(UserFormAttributes userFormAttributes, String id) {
        log.info("Updating user: id={}", id);
        userRepository.update(userFormAttributes, id);
    }

    public boolean isUserExist(String userName) {
        return userRepository.findByUserName(userName).isPresent();
    }
}
