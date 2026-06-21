package com.leaderboard.service;

import com.leaderboard.model.Device;
import com.leaderboard.repository.DeviceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    public Device createDevice(Device device) {
        if (deviceRepository.findByName(device.getName()).isPresent()) {
            throw new IllegalArgumentException("Device name already exists: " + device.getName());
        }
        return deviceRepository.save(device);
    }

    public Device updateDevice(Device device) {
        Device existing = deviceRepository.findByName(device.getName())
                .orElseThrow(() -> new IllegalArgumentException("Device not found with name: " + device.getName()));
        existing.setFeatures(device.getFeatures());
        return deviceRepository.save(existing);
    }
}
