package com.smartwatch.leaderboard.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceCapabilityRequest {

    @NotBlank
    private String capabilityCode;

    @NotBlank
    private String capabilityName;
}