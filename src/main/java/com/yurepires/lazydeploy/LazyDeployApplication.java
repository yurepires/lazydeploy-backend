package com.yurepires.lazydeploy;

import com.yurepires.lazydeploy.config.LazyDeployProperties;
import com.yurepires.lazydeploy.config.BusinessLimitProperties;
import com.yurepires.lazydeploy.config.ProviderProperties;
import com.yurepires.lazydeploy.config.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        LazyDeployProperties.class,
        RateLimitProperties.class,
        BusinessLimitProperties.class,
        ProviderProperties.class
})
public class LazyDeployApplication {

    public static void main(String[] args) {
        SpringApplication.run(LazyDeployApplication.class, args);
    }

}
