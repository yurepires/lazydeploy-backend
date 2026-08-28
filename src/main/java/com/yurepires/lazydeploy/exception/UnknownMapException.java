package com.yurepires.lazydeploy.exception;

import java.util.Collection;
import java.util.stream.Collectors;

public class UnknownMapException extends ApplicationException {

    public UnknownMapException(Collection<String> mapIds) {
        super(
                "UNKNOWN_MAP",
                "One or more map identifiers are not recognized: "
                        + mapIds.stream().collect(Collectors.joining(", "))
        );
    }
}
