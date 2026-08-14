package com.yurepires.lazydeploy.infrastructure.battlelogkeeper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KeeperTeamInfo(Integer faction, Map<String, Object> players) {}
