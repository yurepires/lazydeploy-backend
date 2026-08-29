package com.yurepires.lazydeploy.service.validation.channel;

import com.yurepires.lazydeploy.exception.InvalidChannelConfigurationException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EmailChannelConfigurationValidator implements ChannelConfigurationValidator {

    @Override
    public String supportedType() {
        return "EMAIL";
    }

    @Override
    public void validate(Map<String, Object> parameters) {
        if (parameters != null && !parameters.isEmpty()) {
            throw new InvalidChannelConfigurationException(
                    "EMAIL não aceita parâmetros de configuração"
            );
        }
    }
}
