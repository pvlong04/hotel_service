package org.example.hotel_service.config;

import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_URL = {
            "/auth/register",
            "/auth/login",
            "/auth/refresh",
            "/auth/logout",
            "/auth/token",
            "/auth/introspect",
            "/auth/resend-verification",
            "/auth/forgot-password",
            "/auth/reset-password"
    };

    private final JwtProperties jwtProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD, DispatcherType.ASYNC).permitAll()
                .requestMatchers("/error", "/error/**").permitAll()
                .requestMatchers(HttpMethod.POST, PUBLIC_URL )
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/payments/vnpay/confirm")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/verify-email")
                .permitAll()

                .requestMatchers(HttpMethod.POST, "/ai/chat/**")
                .permitAll()

                // WebSocket endpoint
                .requestMatchers("/ws/**")
                .permitAll()

                // Public GET endpoints
                .requestMatchers(HttpMethod.GET, "/rooms/**", "/room-types/**", "/amenities/**", "/images/**")
                .permitAll()

//                .requestMatchers(HttpMethod.GET, PUBLIC_URL_USER).permitAll()

                .anyRequest().authenticated());


        httpSecurity.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwtConfigurer -> jwtConfigurer.decoder(jwtDecoder()))
        );

        httpSecurity.cors(cors -> cors.configurationSource(request -> {
            var config = new org.springframework.web.cors.CorsConfiguration();
            config.setAllowedOrigins(java.util.List.of("http://localhost:5173", "http://localhost:3000"));
            config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(java.util.List.of("*"));
            config.setAllowCredentials(true);
            return config;
        }));

        httpSecurity.csrf(AbstractHttpConfigurer::disable);
        return httpSecurity.build();
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
