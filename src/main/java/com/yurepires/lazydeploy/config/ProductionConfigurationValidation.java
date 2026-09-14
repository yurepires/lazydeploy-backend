package com.yurepires.lazydeploy.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/** Valida a configuração efetiva antes de iniciar o perfil de produção. */
@Configuration
@Profile("prod")
public class ProductionConfigurationValidation {

    private final Environment environment;

    public ProductionConfigurationValidation(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        requireText("spring.datasource.url");
        requireText("spring.datasource.username");
        requireText("spring.datasource.password");
        requireText("lazydeploy.mailjet.api-key");
        requireText("lazydeploy.mailjet.api-secret");
        requireText("lazydeploy.mailjet.from-email");
        requireText("lazydeploy.security.cors.allowed-origins[0]");

        int mailjetConnectTimeout = requireInteger("lazydeploy.mailjet.connect-timeout-ms");
        if (mailjetConnectTimeout < 1) {
            throw new IllegalStateException(
                    "A propriedade lazydeploy.mailjet.connect-timeout-ms deve ser positiva"
            );
        }

        int mailjetResponseTimeout = requireInteger("lazydeploy.mailjet.response-timeout-ms");
        if (mailjetResponseTimeout < 1) {
            throw new IllegalStateException(
                    "A propriedade lazydeploy.mailjet.response-timeout-ms deve ser positiva"
            );
        }

        int serverPort = requireInteger("server.port");
        if (serverPort < 1 || serverPort > 65_535) {
            throw new IllegalStateException(
                    "A propriedade server.port deve estar entre 1 e 65535"
            );
        }
    }

    private void requireText(String propertyName) {
        String value = environment.getProperty(propertyName);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(
                    "Configuração obrigatória ausente: " + propertyName
            );
        }
    }

    private int requireInteger(String propertyName) {
        String value = environment.getProperty(propertyName);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(
                    "Configuração obrigatória ausente: " + propertyName
            );
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Configuração numérica inválida: " + propertyName,
                    exception
            );
        }
    }
}
