package com.hpote.backend.notification;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hpote.backend.common.ApiExceptionDto;
import com.hpote.backend.notification.model.AppNotification;
import com.hpote.backend.notification.repository.AppNotificationRepository;
import com.hpote.backend.notification.service.NotificationService;
import com.hpote.backend.user.model.User;
import com.hpote.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private AppNotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void usersCannotReadAnotherUsersNotification() {
        User caller = user("caller-id", "caller@example.com");
        User recipient = user("recipient-id", "recipient@example.com");
        AppNotification notification = AppNotification.builder()
                .recipient(recipient)
                .type("NEW_MESSAGE")
                .title("New message")
                .body("Someone sent you a message")
                .build();
        notification.setId("notification-id");

        when(userRepository.findByEmail(caller.getEmail())).thenReturn(Optional.of(caller));
        when(notificationRepository.findById(notification.getId()))
                .thenReturn(Optional.of(notification));

        assertThrows(ApiExceptionDto.class,
                () -> notificationService.markRead(caller.getEmail(), notification.getId()));
    }

    private User user(String id, String email) {
        User user = User.builder().name("HpOtE member").email(email).build();
        user.setId(id);
        return user;
    }
}
