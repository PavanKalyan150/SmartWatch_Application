package com.smartwatch.leaderboard.repository;
import com.smartwatch.leaderboard.model.DeviceCapability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceCapabilityRepository extends JpaRepository<DeviceCapability, Long> {

    List<DeviceCapability> findByDeviceId(Long deviceId);

    boolean existsByDeviceIdAndCapabilityCode(Long deviceId, String capabilityCode);

    Optional<DeviceCapability> findByDeviceIdAndCapabilityCode(Long deviceId, String capabilityCode);
}
