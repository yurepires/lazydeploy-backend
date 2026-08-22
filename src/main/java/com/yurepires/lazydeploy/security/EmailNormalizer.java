package com.yurepires.lazydeploy.security;

import com.yurepires.lazydeploy.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class EmailNormalizer {

    public String normalize(String email) {
        if (email == null) {
            throw new InvalidRequestException("Email é obrigatório");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isBlank()) {
            throw new InvalidRequestException("Email é obrigatório");
        }

        return normalizedEmail;
    }
}
