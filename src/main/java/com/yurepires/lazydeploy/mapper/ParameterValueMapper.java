package com.yurepires.lazydeploy.mapper;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Collection;
import java.util.Map;

@Component
public class ParameterValueMapper {

    private final ObjectMapper objectMapper;

    public ParameterValueMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EncodedParameterValue encode(Object value) {
        if (value instanceof Collection<?> || value instanceof Map<?, ?>) {
            String jsonValue = objectMapper.writeValueAsString(value);
            return new EncodedParameterValue(jsonValue, "JSON");
        }

        if (value instanceof Number) {
            return new EncodedParameterValue(String.valueOf(value), "INTEGER");
        }

        if (value instanceof Boolean) {
            return new EncodedParameterValue(String.valueOf(value), "BOOLEAN");
        }

        return new EncodedParameterValue(String.valueOf(value), "STRING");
    }

    public Object decode(String value, String type) {
        if ("JSON".equals(type)) {
            return objectMapper.readValue(value, Object.class);
        }

        if ("INTEGER".equals(type)) {
            return Integer.valueOf(value);
        }

        if ("BOOLEAN".equals(type)) {
            return Boolean.valueOf(value);
        }

        return value;
    }
}
