package com.yurepires.lazydeploy.integration.mailjet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MailjetMessage(
        @JsonProperty("From") MailjetEmailAddress from,
        @JsonProperty("To") List<MailjetEmailAddress> to,
        @JsonProperty("Subject") String subject,
        @JsonProperty("TextPart") String textPart
) {
}
