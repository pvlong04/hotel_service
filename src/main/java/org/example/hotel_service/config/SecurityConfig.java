package org.example.hotel_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_POST_URLS = {
            "/auth/register", "/auth/login", "/auth/refresh",
            "/auth/logout", "/auth/token", "/auth/introspect"
    };

    private final JwtProperties jwtProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) {
        httpSecurity.authorizeHttpRequests(auth -> auth

                // Auth endpoints - public
                .requestMatchers(HttpMethod.POST, PUBLIC_POST_URLS).permitAll()

                // GET /users/me - tất cả role đã đăng nhập
                .requestMatchers(HttpMethod.GET, "/users/me").authenticated()

                // GET /users - ADMIN hoặc STAFF
                .requestMatchers(HttpMethod.GET, "/users")
                .hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")

                // GET /users/{id} - ADMIN/STAFF xem tất cả, GUEST xem chính mình (service check ownership)
                .requestMatchers(HttpMethod.GET, "/users/{userId}")
                .hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF", "ROLE_GUEST")

                // POST /users - chỉ ADMIN
                .requestMatchers(HttpMethod.POST, "/users")
                .hasAuthority("ROLE_ADMIN")

                // PUT /users/{id} - ADMIN, STAFF, GUEST (service tự kiểm tra ownership)
                .requestMatchers(HttpMethod.PUT, "/users/{userId}")
                .hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF", "ROLE_GUEST")

                // DELETE /users/{id} - chỉ ADMIN
                .requestMatchers(HttpMethod.DELETE, "/users/{userId}")
                .hasAuthority("ROLE_ADMIN")

                // PATCH /users/{id}/status - chỉ ADMIN
                .requestMatchers(HttpMethod.PATCH, "/users/{userId}/status")
                .hasAuthority("ROLE_ADMIN")

                // Rooms read endpoints - public
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/rooms",
                        "/api/v1/rooms/{id}",
                        "/api/v1/rooms/available",
                        "/api/v1/rooms/{id}/images")
                .permitAll()

                // Rooms write endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/rooms")
                .hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/rooms/{id}")
                .hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/rooms/{id}")
                .hasAuthority("ROLE_ADMIN")

                .anyRequest().authenticated()
        );

        httpSecurity.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwtConfigurer -> jwtConfigurer
                        .decoder(jwtDecoder())
                        .jwtAuthenticationConverter(jwtAuthenticationConverter()))
        );

        httpSecurity.csrf(AbstractHttpConfigurer::disable);
        return httpSecurity.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object rolesObj = jwt.getClaims().get("roles");
            if (rolesObj instanceof Collection<?> roleCollection) {
                List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities = new ArrayList<>();
                for (Object role : roleCollection) {
                    if (role != null) {
                        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
                    }
                }
                if (!authorities.isEmpty()) {
                    return authorities;
                }
            }

            // Fallback for legacy token that still uses single "role" claim.
            Object roleObj = jwt.getClaims().get("role");
            if (roleObj == null) return Collections.emptyList();
            return List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + roleObj));
        });
        return jwtConverter;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(jwtProperties.getSignerKey().getBytes(), "HS512");
        return NimbusJwtDecoder
                .withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}


