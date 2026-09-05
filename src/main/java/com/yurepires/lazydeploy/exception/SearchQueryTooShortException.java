package com.yurepires.lazydeploy.exception;

public class SearchQueryTooShortException extends ApplicationException {

    public SearchQueryTooShortException() {
        this(2);
    }

    public SearchQueryTooShortException(int minimumLength) {
        super(
                "SEARCH_QUERY_TOO_SHORT",
                "A consulta de busca deve conter pelo menos "
                        + minimumLength
                        + " caracteres."
        );
    }
}
