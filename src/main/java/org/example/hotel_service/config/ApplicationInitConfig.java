package org.example.hotel_service.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.hotel_service.entities.Role;
import org.example.hotel_service.entities.User;
import org.example.hotel_service.entities.UserRole;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.enums.UserStatus;
import org.example.hotel_service.repositories.RoleRepository;
import org.example.hotel_service.repositories.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner(
            UserRepository repo,
            RoleRepository roleRepository,
            @Value("${app.bootstrap.admin.enabled:false}") boolean bootstrapEnabled,
            @Value("${app.bootstrap.admin.username:admin}") String bootstrapUsername,
            @Value("${app.bootstrap.admin.email:admin@hotel.local}") String bootstrapEmail,
            @Value("${app.bootstrap.admin.password:}") String bootstrapPassword
    ) {
        return args -> {
            if (!bootstrapEnabled) {
                log.info("Admin bootstrap is disabled. Skip seeding default admin user.");
                return;
            }

            if (!StringUtils.hasText(bootstrapPassword) || bootstrapPassword.length() < 8) {
                log.warn("Admin bootstrap is enabled but BOOTSTRAP_ADMIN_PASSWORD is missing/weak (min 8 chars). Skip seeding admin.");
                return;
            }

            Role adminRole = roleRepository.findByName(Roles.ADMIN)
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .name(Roles.ADMIN)
                            .build()));

            User user = repo.findWithProfileAndRolesByUsernameOrEmail(bootstrapUsername, bootstrapEmail).orElse(null);

            if (user == null) {
                User admin = User.builder()
                        .username(bootstrapUsername)
                        .email(bootstrapEmail)
                        .passwordHash(passwordEncoder.encode(bootstrapPassword))
                        .status(UserStatus.ACTIVE)
                        .build();

                UserRole adminUserRole = UserRole.builder()
                        .user(admin)
                        .role(adminRole)
                        .build();

                Set<UserRole> roles = new HashSet<>();
                roles.add(adminUserRole);
                admin.setUserRoles(roles);
                repo.save(admin);
                log.info("Bootstrap admin user '{}' has been created.", bootstrapUsername);
                return;
            }

            Set<UserRole> currentRoles = user.getUserRoles() == null
                    ? new HashSet<>()
                    : new HashSet<>(user.getUserRoles());

            boolean hasAdminRole = currentRoles.stream()
                    .anyMatch(ur -> ur.getRole() != null && ur.getRole().getName() == Roles.ADMIN);

            boolean changed = false;
            if (!hasAdminRole) {
                currentRoles.add(UserRole.builder()
                        .user(user)
                        .role(adminRole)
                        .build());
                user.setUserRoles(currentRoles);
                changed = true;
            }

            if (user.getStatus() != UserStatus.ACTIVE) {
                user.setStatus(UserStatus.ACTIVE);
                changed = true;
            }

            if (changed) {
                repo.save(user);
                log.info("Bootstrap user '{}' has been promoted/activated as ADMIN.", user.getUsername());
            }
        };
    }
}
