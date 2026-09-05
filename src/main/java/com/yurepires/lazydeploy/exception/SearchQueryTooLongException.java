package com.yurepires.lazydeploy.exception;

public class SearchQueryTooLongException extends ApplicationException {

    public SearchQueryTooLongException() {
        this(100);
    }

    public SearchQueryTooLongException(int maximumLength) {
        super(
                "SEARCH_QUERY_TOO_LONG",
                "A consulta de busca não pode exceder "
                        + maximumLength
                        + " caracteres."
        );
    }
}
