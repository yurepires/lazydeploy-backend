package com.yurepires.lazydeploy.integration.mailjet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MailjetSendResponse(
        @JsonProperty("Messages") List<MailjetMessageResult> messages
) {
}
