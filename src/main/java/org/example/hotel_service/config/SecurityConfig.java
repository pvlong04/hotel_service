package org.example.hotel_service.config;

import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.List;

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

    @Value("${app.cors.allowed-origin-patterns:http://localhost:3000,http://localhost:5173,https://*.ngrok-free.dev,https://*.onrender.com,https://*.vercel.app,https://*.netlify.app}")
    private String allowedOriginPatterns;

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
                .requestMatchers(HttpMethod.GET, "/rooms/**", "/room-types/**", "/amenities/**", "/images/**", "/reviews/public/**")
                .permitAll()

//                .requestMatchers(HttpMethod.GET, PUBLIC_URL_USER).permitAll()

                .anyRequest().authenticated());


        httpSecurity.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwtConfigurer -> jwtConfigurer.decoder(jwtDecoder()))
        );

        httpSecurity.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        httpSecurity.csrf(AbstractHttpConfigurer::disable);
        return httpSecurity.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(parseOriginPatterns());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> parseOriginPatterns() {
        return Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
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
