package com.yurepires.lazydeploy.service.validation;

import com.yurepires.lazydeploy.config.BusinessLimitProperties;
import com.yurepires.lazydeploy.exception.SearchQueryTooLongException;
import com.yurepires.lazydeploy.exception.SearchQueryTooShortException;
import com.yurepires.lazydeploy.service.observability.BusinessLimitMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServerSearchQueryPolicyTest {

    private final ServerSearchQueryPolicy policy = new ServerSearchQueryPolicy(
            new BusinessLimitProperties(20, 10, 5, 20, 2, 5, 20, 100, 10000, 1048576),
            new BusinessLimitMetrics(new SimpleMeterRegistry())
    );

    @Test
    void shouldTrimValidQuery() {
        assertThat(policy.validate("  lost  ")).isEqualTo("lost");
    }

    @Test
    void shouldRejectQueriesOutsideConfiguredLength() {
        assertThatThrownBy(() -> policy.validate("x"))
                .isInstanceOf(SearchQueryTooShortException.class);
        assertThatThrownBy(() -> policy.validate("abcdef"))
                .isInstanceOf(SearchQueryTooLongException.class);
    }
}
