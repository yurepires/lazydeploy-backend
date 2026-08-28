package com.yurepires.lazydeploy.service.validation.channel;

import com.yurepires.lazydeploy.exception.InvalidChannelConfigurationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class EmailChannelConfigurationValidator implements ChannelConfigurationValidator {

    private final Validator validator;

    public EmailChannelConfigurationValidator(Validator validator) {
        this.validator = validator;
    }

    @Override
    public String supportedType() {
        return "EMAIL";
    }

    @Override
    public void validate(Map<String, Object> parameters) {
        if (parameters == null) {
            throw new InvalidChannelConfigurationException(
                    "Os parâmetros do EMAIL devem ser informados"
            );
        }

        if (parameters.size() != 1 || !parameters.containsKey("recipient")) {
            throw new InvalidChannelConfigurationException(
                    "EMAIL aceita somente o parâmetro 'recipient'"
            );
        }

        Object recipient = parameters.get("recipient");
        if (!(recipient instanceof String recipientText)) {
            throw new InvalidChannelConfigurationException(
                    "O destinatário EMAIL deve ser um texto"
            );
        }

        Set<ConstraintViolation<EmailAddress>> violations = validator.validate(
                new EmailAddress(recipientText)
        );
        if (!violations.isEmpty()) {
            throw new InvalidChannelConfigurationException(
                    "O destinatário EMAIL deve ser um endereço válido"
            );
        }
    }

    private record EmailAddress(
            @NotBlank @Email String value
    ) {
    }
}
