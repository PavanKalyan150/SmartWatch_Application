package com.smartwatch.leaderboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.util.List;
@Getter
@Builder
@AllArgsConstructor
public class DeviceResponse {

    private Long id;
    private String deviceName;
    private String manufacturer;
    private String model;
    private List<String> capabilities;
}