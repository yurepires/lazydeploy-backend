package com.yurepires.lazydeploy.service.validation;

import com.yurepires.lazydeploy.config.BusinessLimitProperties;
import com.yurepires.lazydeploy.exception.SubscriptionLimitReachedException;
import com.yurepires.lazydeploy.repository.ServerSubscriptionRepository;
import com.yurepires.lazydeploy.service.observability.BusinessLimitMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessLimitServiceTest {

    @Test
    void shouldRejectSubscriptionWhenUserAlreadyReachedLimit() {
        ServerSubscriptionRepository repository = mock(ServerSubscriptionRepository.class);
        BusinessLimitMetrics metrics = new BusinessLimitMetrics(new SimpleMeterRegistry());
        BusinessLimitProperties properties = new BusinessLimitProperties(
                2, 10, 5, 20, 2, 100, 20, 100, 10000, 1048576
        );
        BusinessLimitService service = new BusinessLimitService(repository, properties, metrics);
        UUID userId = UUID.randomUUID();
        when(repository.countByUserId(userId)).thenReturn(2L);

        assertThatThrownBy(() -> service.ensureSubscriptionCapacity(userId))
                .isInstanceOf(SubscriptionLimitReachedException.class)
                .hasMessage("Você atingiu o número máximo de alertas permitidos.");

        verify(repository).countByUserId(userId);
    }
}
