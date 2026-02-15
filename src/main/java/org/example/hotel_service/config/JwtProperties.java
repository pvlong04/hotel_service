package org.example.hotel_service.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JwtProperties {
    String signerKey;
    Integer accessTokenMinutes = 60;
    Integer refreshTokenDays = 7;
}
