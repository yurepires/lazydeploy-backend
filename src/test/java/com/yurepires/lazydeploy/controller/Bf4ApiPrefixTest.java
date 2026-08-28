package com.yurepires.lazydeploy.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Bf4ApiPrefixTest {
    @Test
    void shouldUseApiBf4PrefixForEveryPublicBf4Controller() {
        List<Class<?>> controllers = List.of(
                ServerSearchController.class,
                SubscriptionController.class,
                BattlefieldMapController.class
        );

        for (Class<?> controller : controllers) {
            RequestMapping mapping = controller.getAnnotation(RequestMapping.class);

            assertThat(mapping).isNotNull();
            assertThat(mapping.value()).allMatch(path -> path.startsWith("/api/bf4/"));
        }
    }
}
