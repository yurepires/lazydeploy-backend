package com.yurepires.lazydeploy.service.validation;

import com.yurepires.lazydeploy.config.BusinessLimitProperties;
import com.yurepires.lazydeploy.exception.SearchQueryTooLongException;
import com.yurepires.lazydeploy.exception.SearchQueryTooShortException;
import com.yurepires.lazydeploy.service.observability.BusinessLimitMetrics;
import org.springframework.stereotype.Component;

/** Normaliza e valida a consulta antes de chamar o provider externo. */
@Component
public class ServerSearchQueryPolicy {

    private final BusinessLimitProperties properties;
    private final BusinessLimitMetrics metrics;

    public ServerSearchQueryPolicy(
            BusinessLimitProperties properties,
            BusinessLimitMetrics metrics
    ) {
        this.properties = properties;
        this.metrics = metrics;
    }

    public String validate(String query) {
        String normalizedQuery = normalizeQuery(query);

        if (normalizedQuery.length() < properties.serverSearchQueryMinLength()) {
            metrics.recordRejection("search_query");
            throw new SearchQueryTooShortException(
                    properties.serverSearchQueryMinLength()
            );
        }

        if (normalizedQuery.length() > properties.serverSearchQueryMaxLength()) {
            metrics.recordRejection("search_query");
            throw new SearchQueryTooLongException(
                    properties.serverSearchQueryMaxLength()
            );
        }

        return normalizedQuery;
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }

        return query.trim();
    }
}
