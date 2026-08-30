package com.smartwatch.leaderboard.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DeviceRequest {

    @NotBlank
    private String deviceName;

    @NotBlank
    private String manufacturer;

    @NotBlank
    private String model;

    @NotEmpty
    private List<String> capabilities;
}