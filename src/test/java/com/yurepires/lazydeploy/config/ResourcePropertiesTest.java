package com.yurepires.lazydeploy.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ResourcePropertiesTest {

    @Test
    void shouldProvideSafeDefaultsWhenSectionsAreMissing() {
        ResourceProperties properties = new ResourceProperties(null, null, null, null);

        assertThat(properties.database().maximumPoolSize()).isEqualTo(10);
        assertThat(properties.database().connectionTimeoutMs()).isEqualTo(3_000);
        assertThat(properties.monitoringExecutor().queueCapacity()).isEqualTo(100);
        assertThat(properties.shutdown().timeout()).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void shouldRejectAnExecutorCorePoolLargerThanItsMaximum() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ResourceProperties.MonitoringExecutor(5, 4, 10, 60));
    }

    @Test
    void shouldRejectAnIdleDatabasePoolLargerThanItsMaximum() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ResourceProperties.Database(4, 5, 3_000, 60_000, 300_000));
    }
}
