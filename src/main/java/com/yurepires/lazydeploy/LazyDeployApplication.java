package com.yurepires.lazydeploy;

import com.yurepires.lazydeploy.config.LazyDeployProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(LazyDeployProperties.class)
public class LazyDeployApplication {

    public static void main(String[] args) {
        SpringApplication.run(LazyDeployApplication.class, args);
    }

}
