package com.smartwatch.leaderboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceCapabilityResponse {

    private Long id;
    private Long deviceId;
    private String capabilityCode;
    private String capabilityName;
}