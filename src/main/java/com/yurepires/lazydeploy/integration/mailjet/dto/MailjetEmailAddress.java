package com.yurepires.lazydeploy.integration.mailjet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MailjetEmailAddress(
        @JsonProperty("Email") String email,
        @JsonProperty("Name") String name
) {
}
