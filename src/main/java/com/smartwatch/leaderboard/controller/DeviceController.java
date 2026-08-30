package com.smartwatch.leaderboard.controller;

import com.smartwatch.leaderboard.dto.request.DeviceRequest;
import com.smartwatch.leaderboard.dto.response.DeviceResponse;
import com.smartwatch.leaderboard.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController {

    private static final Logger log = LoggerFactory.getLogger(DeviceController.class);

    private final DeviceService deviceService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceResponse> createDevice(@Valid @RequestBody DeviceRequest request) {
        log.debug("Registering device: {}", request.getDeviceName());
        DeviceResponse response = deviceService.createDevice(request);
        log.info("Device registered with id: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<DeviceResponse>> getAllDevices() {
        log.debug("Fetching all devices");
        List<DeviceResponse> response = deviceService.getAllDevices();
        log.info("Fetched {} devices", response.size());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceResponse> updateDevice(@PathVariable Long id,
                                                       @Valid @RequestBody DeviceRequest request) {
        log.debug("Updating device id: {}", id);
        DeviceResponse response = deviceService.updateDevice(id, request);
        log.info("Device updated with id: {}", id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}