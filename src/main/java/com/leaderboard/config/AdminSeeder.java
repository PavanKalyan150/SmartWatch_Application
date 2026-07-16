package com.leaderboard.config;

import com.leaderboard.model.Role;
import com.leaderboard.model.User;
import com.leaderboard.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String adminPhone = "9999999999";
        if (userRepository.findByPhone(adminPhone).isEmpty()) {
            User admin = new User();
            admin.setPhone(adminPhone);
            admin.setEmail("admin@pulseiq.com");
            admin.setFullName("System Admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ROLE_ADMIN);
            userRepository.save(admin);
            System.out.println("=== PULSE.IQ: Default admin account seeded ===");
            System.out.println("  Phone: " + adminPhone);
            System.out.println("  Password: admin123");
            System.out.println("===============================================");
        }
    }
}
