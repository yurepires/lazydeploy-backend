package com.yurepires.lazydeploy.service.notification;

import com.yurepires.lazydeploy.model.notification.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationChannelRegistry {

    private final List<NotificationChannel> channels;

    public NotificationChannelRegistry(List<NotificationChannel> channels) {
        this.channels = List.copyOf(channels);
    }

    public NotificationChannel resolve(String type) {
        return channels.stream()
                .filter(channel -> channel.type().equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nenhum NotificationChannel registrado para o tipo " + type
                ));
    }
}
