package com.yurepires.lazydeploy.integration.mailjet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MailjetMessageResult(
        @JsonProperty("Status") String status
) {
}
