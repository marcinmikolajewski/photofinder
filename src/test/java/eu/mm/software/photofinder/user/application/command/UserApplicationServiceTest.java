package eu.mm.software.photofinder.user.application.command;

import eu.mm.software.photofinder.user.domain.UserFormAttributes;

import eu.mm.software.photofinder.photosattribute.domain.event.DomainEventPublisher;
import eu.mm.software.photofinder.user.domain.User;
import eu.mm.software.photofinder.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserApplicationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    private UserApplicationService userApplicationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userApplicationService = new UserApplicationService(userRepository, eventPublisher);
    }

    @Test
    void shouldSaveUserSuccessfully() {
        // Given
        UserFormAttributes userFormAttributes = createTestUserFormAttributes();
        String expectedId = "generated-id-123";
        when(userRepository.save(userFormAttributes)).thenReturn(expectedId);

        // When
        String result = userApplicationService.save(userFormAttributes);

        // Then
        assertThat(result).isEqualTo(expectedId);
        verify(userRepository).save(userFormAttributes);
    }

    @Test
    void shouldDeleteUserSuccessfully() {
        // Given
        String userId = "user-id-123";

        // When
        userApplicationService.delete(userId);

        // Then
        verify(userRepository).delete(userId);
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        // Given
        UserFormAttributes userFormAttributes = createTestUserFormAttributes();
        String userId = "user-id-123";

        // When
        userApplicationService.update(userFormAttributes, userId);

        // Then
        verify(userRepository).update(userFormAttributes, userId);
    }

    @Test
    void shouldReturnTrueWhenUserExists() {
        // Given
        String userName = "existingUser";
        User existingUser = new User();
        existingUser.setUserName(userName);
        when(userRepository.findByUserName(userName)).thenReturn(Optional.of(existingUser));

        // When
        boolean result = userApplicationService.isUserExist(userName);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).findByUserName(userName);
    }

    @Test
    void shouldReturnFalseWhenUserDoesNotExist() {
        // Given
        String userName = "nonExistentUser";
        when(userRepository.findByUserName(userName)).thenReturn(Optional.empty());

        // When
        boolean result = userApplicationService.isUserExist(userName);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).findByUserName(userName);
    }

    private UserFormAttributes createTestUserFormAttributes() {
        return new UserFormAttributes() {
            @Override
            public String getId() {
                return null;
            }

            @Override
            public String getUserName() {
                return "testUser";
            }

            @Override
            public String getUserEmail() {
                return "test@example.com";
            }

            @Override
            public String getPasswordForm1() {
                return "password123";
            }

            @Override
            public String getPasswordForm2() {
                return "password123";
            }

            @Override
            public Boolean getActive() {
                return true;
            }

            @Override
            public Set<String> getRoles() {
                return Set.of("USER");
            }
        };
    }
}