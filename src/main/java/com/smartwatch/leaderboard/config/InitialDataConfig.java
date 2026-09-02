package com.smartwatch.leaderboard.config;

import com.smartwatch.leaderboard.model.Device;
import com.smartwatch.leaderboard.model.Level;
import com.smartwatch.leaderboard.repository.DeviceRepository;
import com.smartwatch.leaderboard.repository.LevelRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InitialDataConfig {

    @Bean
    CommandLineRunner seedInitialRegistrationData(LevelRepository levelRepository,
                                                   DeviceRepository deviceRepository) {
        return args -> {
            if (levelRepository.count() == 0) {
                levelRepository.save(Level.builder().levelName("Beginner").pointThreshold(0).build());
            }
            if (deviceRepository.count() == 0) {
                deviceRepository.save(Device.builder()
                        .deviceName("PulseTrack Starter")
                        .manufacturer("PulseTrack")
                        .model("PT-1")
                        .build());
            }
        };
    }
}
