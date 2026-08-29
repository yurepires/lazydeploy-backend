package com.yurepires.lazydeploy.service.notification;

import com.yurepires.lazydeploy.exception.NotificationRecipientUnavailableException;
import com.yurepires.lazydeploy.model.notification.NotificationRecipientResolver;
import com.yurepires.lazydeploy.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserEmailNotificationRecipientResolver implements NotificationRecipientResolver {

    private final UserRepository userRepository;

    public UserEmailNotificationRecipientResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public String resolveEmail(UUID userId) {
        if (userId == null) {
            throw new NotificationRecipientUnavailableException();
        }

        return userRepository.findById(userId)
                .filter(user -> user.isEnabled())
                .map(user -> user.getEmail())
                .filter(email -> email != null && !email.isBlank())
                .orElseThrow(NotificationRecipientUnavailableException::new);
    }
}
