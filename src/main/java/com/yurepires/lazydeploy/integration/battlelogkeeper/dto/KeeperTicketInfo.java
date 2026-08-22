package com.yurepires.lazydeploy.integration.battlelogkeeper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KeeperTicketInfo(
        Integer tickets,
        Integer ticketsMax
) {}
