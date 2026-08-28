package com.yurepires.lazydeploy.service.validation.channel;

import java.util.Map;

public interface ChannelConfigurationValidator {

    String supportedType();

    void validate(Map<String, Object> parameters);
}
