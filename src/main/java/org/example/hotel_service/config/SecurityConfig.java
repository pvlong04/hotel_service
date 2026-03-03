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
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;

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
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(auth -> auth

                // Auth endpoints - public
                .requestMatchers(HttpMethod.POST, PUBLIC_POST_URLS).permitAll()

                // GET /users/me - tất cả role đã đăng nhập
                .requestMatchers(HttpMethod.GET, "/users/me").authenticated()

                // GET /users, GET /users/{id} - ADMIN hoặc STAFF
                .requestMatchers(HttpMethod.GET, "/users", "/users/{userId}")
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

    /**
     * Chuyển claim "role" trong JWT thành GrantedAuthority có prefix "ROLE_"
     * VD: "ADMIN" → "ROLE_ADMIN"
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("role");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // claim "role" là string đơn, cần wrap thành collection
            Object roleObj = jwt.getClaims().get("role");
            if (roleObj == null) return java.util.Collections.emptyList();
            String roleValue = roleObj.toString();
            return java.util.List.of(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + roleValue)
            );
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


