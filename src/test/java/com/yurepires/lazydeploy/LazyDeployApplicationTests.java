package com.yurepires.lazydeploy;

import com.yurepires.lazydeploy.config.LazyDeployProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LazyDeployApplicationTests {

    @Autowired
    private LazyDeployProperties properties;

    @Test
    void contextLoads() {
    }

    @Test
    void bindsInfrastructureOnlyConfiguration() {
        assertThat(properties.monitoring().roundResetThresholdSeconds()).isEqualTo(30);
        assertThat(properties.battlelogKeeperBaseUrl()).contains("battlelog.com");
        assertThat(properties.gameToolsBaseUrl()).contains("gametools.network");
        assertThat(properties.emailFrom()).isEqualTo("test@example.com");
    }

}
