package org.example.hotel_service.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.hotel_service.entities.Role;
import org.example.hotel_service.entities.User;
import org.example.hotel_service.entities.UserRole;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.repositories.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner(UserRepository repo, org.example.hotel_service.repositories.RoleRepository roleRepository) {
        return args -> {
            Role adminRole = roleRepository.findByName(Roles.ADMIN)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(Roles.ADMIN).build()));

            if (!repo.existsByUsername("admin")) {
                User admin = User.builder()
                        .username("admin")
                        .passwordHash(passwordEncoder.encode("admin"))
                        .build();

                UserRole adminUserRole = UserRole.builder()
                        .user(admin)
                        .role(adminRole)
                        .build();

                admin.setUserRoles(List.of(adminUserRole));
                repo.save(admin);
            }
        };
    }

}
