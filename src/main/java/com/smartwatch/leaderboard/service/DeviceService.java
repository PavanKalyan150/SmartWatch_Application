package com.smartwatch.leaderboard.service;

import com.smartwatch.leaderboard.dto.request.DeviceCapabilityRequest;
import com.smartwatch.leaderboard.dto.request.DeviceRequest;
import com.smartwatch.leaderboard.dto.response.DeviceCapabilityResponse;
import com.smartwatch.leaderboard.dto.response.DeviceResponse;
import com.smartwatch.leaderboard.model.Device;
import com.smartwatch.leaderboard.model.DeviceCapability;
import com.smartwatch.leaderboard.repository.DeviceCapabilityRepository;
import com.smartwatch.leaderboard.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceCapabilityRepository deviceCapabilityRepository;

    @Transactional
    public DeviceResponse createDevice(DeviceRequest request) {
        boolean duplicate = deviceRepository.existsByDeviceNameAndManufacturerAndModel(
                request.getDeviceName(),
                request.getManufacturer(),
                request.getModel()
        );

        if (duplicate) {
            throw new IllegalArgumentException(
                    "Device already exists with the same name, manufacturer and model");
        }

        Device device = Device.builder()
                .deviceName(request.getDeviceName())
                .manufacturer(request.getManufacturer())
                .model(request.getModel())
                .build();

        if (request.getCapabilities() != null && !request.getCapabilities().isEmpty()) {
            List<DeviceCapability> caps = request.getCapabilities().stream()
                    .filter(code -> code != null && !code.isBlank())
                    .distinct()
                    .map(code -> DeviceCapability.builder()
                            .device(device)              // back-reference for FK
                            .capabilityCode(code)
                            .build())
                    .toList();
            device.getCapabilities().addAll(caps);
        }

        deviceRepository.save(device);   // cascade = ALL persists capabilities too
        return mapToDeviceResponse(device);
    }

    @Transactional
    public DeviceResponse updateDevice(Long id, DeviceRequest request) {
        Device device = findDeviceById(id);

        device.setDeviceName(request.getDeviceName());
        device.setManufacturer(request.getManufacturer());
        device.setModel(request.getModel());
        if (request.getCapabilities() != null && !request.getCapabilities().isEmpty()) {
            List<DeviceCapability> caps = request.getCapabilities().stream()
                    .filter(code -> code != null && !code.isBlank())
                    .distinct()
                    .map(code -> DeviceCapability.builder()
                            .device(device)              // back-reference for FK
                            .capabilityCode(code)
                            .build())
                    .toList();
            device.getCapabilities().addAll(caps);
        }
        deviceRepository.save(device);
        return mapToDeviceResponse(device);
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> getAllDevices() {
        return deviceRepository.findAll()
                .stream()
                .map(this::mapToDeviceResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeviceCapabilityResponse addCapability(Long deviceId, DeviceCapabilityRequest request) {
        Device device = findDeviceById(deviceId);

        boolean alreadyExists = deviceCapabilityRepository
                .existsByDeviceIdAndCapabilityCode(deviceId, request.getCapabilityCode());

        if (alreadyExists) {
            throw new IllegalArgumentException("Capability already exists for this device: " + request.getCapabilityCode());
        }

        DeviceCapability capability = DeviceCapability.builder()
                .device(device)
                .capabilityCode(request.getCapabilityCode())
                .build();

        deviceCapabilityRepository.save(capability);
        return mapToCapabilityResponse(capability);
    }

    @Transactional
    public void removeCapability(Long deviceId, String capabilityCode) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new IllegalArgumentException("Device not found: " + deviceId);
        }

        DeviceCapability capability = deviceCapabilityRepository
                .findByDeviceIdAndCapabilityCode(deviceId, capabilityCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Capability not found: " + capabilityCode + " on device: " + deviceId));

        deviceCapabilityRepository.delete(capability);
    }

    @Transactional(readOnly = true)
    public List<DeviceCapabilityResponse> getCapabilities(Long deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new IllegalArgumentException("Device not found: " + deviceId);
        }

        return deviceCapabilityRepository.findByDeviceId(deviceId)
                .stream()
                .map(this::mapToCapabilityResponse)
                .collect(Collectors.toList());
    }

    // --- private helpers ---

    private Device findDeviceById(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + id));
    }

    private DeviceResponse mapToDeviceResponse(Device device) {
        List<String> capabilityCodes = device.getCapabilities() == null
                ? List.of()
                : device.getCapabilities().stream()
                  .map(DeviceCapability::getCapabilityCode)
                  .collect(Collectors.toList());

        return DeviceResponse.builder()
                .id(device.getId())
                .deviceName(device.getDeviceName())
                .manufacturer(device.getManufacturer())
                .model(device.getModel())
                .capabilities(capabilityCodes)
                .build();
    }

    private DeviceCapabilityResponse mapToCapabilityResponse(DeviceCapability capability) {
        return DeviceCapabilityResponse.builder()
                .id(capability.getId())
                .deviceId(capability.getDevice().getId())
                .capabilityCode(capability.getCapabilityCode())
                .capabilityName(capability.getCapabilityCode())
                .build();
    }
}
