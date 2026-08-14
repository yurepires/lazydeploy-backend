package com.yurepires.lazydeploy.infrastructure.battlelogkeeper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KeeperSnapshotResponse(Long lastUpdated, KeeperSnapshot snapshot) {}
