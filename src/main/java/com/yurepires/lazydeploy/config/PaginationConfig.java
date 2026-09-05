package com.yurepires.lazydeploy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

/** Configura o tamanho padrão do Pageable usado pelos endpoints paginados. */
@Configuration
public class PaginationConfig {

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableDefaults(
            BusinessLimitProperties properties
    ) {
        return resolver -> resolver.setFallbackPageable(
                PageRequest.of(0, properties.defaultPageSize())
        );
    }
}
