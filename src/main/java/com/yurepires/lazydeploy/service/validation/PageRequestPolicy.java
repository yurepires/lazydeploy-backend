package com.yurepires.lazydeploy.service.validation;

import com.yurepires.lazydeploy.config.BusinessLimitProperties;
import com.yurepires.lazydeploy.exception.InvalidPageException;
import com.yurepires.lazydeploy.exception.InvalidPageSizeException;
import com.yurepires.lazydeploy.exception.PageSizeLimitExceededException;
import com.yurepires.lazydeploy.service.observability.PaginationMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/** Aplica os limites comuns de paginação antes de criar um Pageable. */
@Component
public class PageRequestPolicy {

    private final BusinessLimitProperties properties;
    private final PaginationMetrics metrics;

    @Autowired
    public PageRequestPolicy(
            BusinessLimitProperties properties,
            PaginationMetrics metrics
    ) {
        this.properties = properties;
        this.metrics = metrics;
    }

    public PageRequestPolicy() {
        this.properties = new BusinessLimitProperties(
                20,
                10,
                5,
                20,
                2,
                100,
                20,
                100,
                10000,
                1048576
        );
        this.metrics = null;
    }

    public Pageable apply(Pageable pageable, Sort sort) {
        return apply(pageable, sort, null, null);
    }

    public Pageable apply(
            Pageable pageable,
            Sort sort,
            Integer requestedPage,
            Integer pageSizeOverride
    ) {
        int page = 0;
        int requestedSize = properties.defaultPageSize();

        if (pageable != null && !pageable.isUnpaged()) {
            page = pageable.getPageNumber();
            requestedSize = pageable.getPageSize();
        }

        if (requestedPage != null) {
            page = requestedPage;
        }
        if (pageSizeOverride != null) {
            requestedSize = pageSizeOverride;
        }

        validatePage(page);
        validatePageSize(requestedSize);

        Sort effectiveSort = sort;
        if (effectiveSort == null) {
            effectiveSort = Sort.unsorted();
        }

        return PageRequest.of(page, requestedSize, effectiveSort);
    }

    private void validatePage(int page) {
        if (page < 0 || page > properties.maxPageNumber()) {
            recordRejection("invalid_page");
            throw new InvalidPageException(
                    "O número da página deve estar entre 0 e "
                            + properties.maxPageNumber()
            );
        }
    }

    private void validatePageSize(int size) {
        if (size <= 0) {
            recordRejection("invalid_page_size");
            throw new InvalidPageSizeException(
                    "O tamanho da página deve ser positivo"
            );
        }

        if (size > properties.maxPageSize()) {
            recordRejection("page_size_limit_exceeded");
            throw new PageSizeLimitExceededException(
                    "O tamanho da página não pode exceder "
                            + properties.maxPageSize()
            );
        }
    }

    private void recordRejection(String reason) {
        if (metrics != null) {
            metrics.recordRejection(reason);
        }
    }
}
