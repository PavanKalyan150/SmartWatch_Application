package com.smartwatch.leaderboard.repository;
import com.smartwatch.leaderboard.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    boolean existsByDeviceNameAndManufacturerAndModel(String deviceName, String manufacturer, String model);
}