package com.yurepires.lazydeploy.service.validation;

import com.yurepires.lazydeploy.config.BusinessLimitProperties;
import com.yurepires.lazydeploy.exception.InvalidPageException;
import com.yurepires.lazydeploy.exception.InvalidPageSizeException;
import com.yurepires.lazydeploy.exception.PageSizeLimitExceededException;
import com.yurepires.lazydeploy.service.observability.PaginationMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestPolicyTest {

    private final BusinessLimitProperties properties = new BusinessLimitProperties(
            20, 10, 5, 20, 2, 100, 20, 100, 10000, 1048576
    );
    private final PageRequestPolicy policy = new PageRequestPolicy(
            properties,
            new PaginationMetrics(new SimpleMeterRegistry())
    );

    @Test
    void shouldKeepConfiguredPageAndSizeWithinLimits() {
        assertThat(policy.apply(
                        PageRequest.of(2, 50),
                        Sort.by("attemptedAt")
                ).getPageSize())
                .isEqualTo(50);
    }

    @Test
    void shouldRejectInvalidPageAndSize() {
        assertThatThrownBy(() -> policy.apply(
                        PageRequest.of(0, 20),
                        Sort.unsorted(),
                        -1,
                        null
                ))
                .isInstanceOf(InvalidPageException.class);

        assertThatThrownBy(() -> policy.apply(
                        PageRequest.of(0, 20),
                        Sort.unsorted(),
                        null,
                        0
                ))
                .isInstanceOf(InvalidPageSizeException.class);

        assertThatThrownBy(() -> policy.apply(
                        PageRequest.of(0, 20),
                        Sort.unsorted(),
                        null,
                        101
                ))
                .isInstanceOf(PageSizeLimitExceededException.class);
    }
}
