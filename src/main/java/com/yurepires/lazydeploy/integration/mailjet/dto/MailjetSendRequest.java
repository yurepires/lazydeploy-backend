package com.yurepires.lazydeploy.integration.mailjet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MailjetSendRequest(
        @JsonProperty("Messages") List<MailjetMessage> messages
) {
}
