package com.yurepires.lazydeploy.service.notification;

import com.yurepires.lazydeploy.entity.UserEntity;
import com.yurepires.lazydeploy.exception.NotificationRecipientUnavailableException;
import com.yurepires.lazydeploy.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserEmailNotificationRecipientResolverTest {

    @Test
    void shouldResolveCurrentEmailFromSubscriptionOwner() {
        UserRepository userRepository = mock(UserRepository.class);
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity(
                userId,
                "owner@example.com",
                "hashed-password",
                true,
                Instant.now(),
                Instant.now()
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserEmailNotificationRecipientResolver resolver =
                new UserEmailNotificationRecipientResolver(userRepository);

        assertThat(resolver.resolveEmail(userId)).isEqualTo("owner@example.com");
    }

    @Test
    void shouldFailWhenSubscriptionOwnerCannotBeResolved() {
        UserRepository userRepository = mock(UserRepository.class);
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UserEmailNotificationRecipientResolver resolver =
                new UserEmailNotificationRecipientResolver(userRepository);

        assertThatThrownBy(() -> resolver.resolveEmail(userId))
                .isInstanceOf(NotificationRecipientUnavailableException.class);
    }
}
