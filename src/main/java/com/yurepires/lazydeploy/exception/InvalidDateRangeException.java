package com.yurepires.lazydeploy.exception;

import java.time.Instant;

public class InvalidDateRangeException extends InvalidRequestException {

    public InvalidDateRangeException(Instant from, Instant to) {
        super(
                "INVALID_DATE_RANGE",
                "O início do período não pode ser posterior ao fim"
        );
    }
}
