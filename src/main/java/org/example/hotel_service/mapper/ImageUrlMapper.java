package org.example.hotel_service.mapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.config.ImageStorageProperties;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ImageUrlMapper {

    ImageStorageProperties imageStorageProperties;

    @Named("normalizeImageUrl")
    public String normalize(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }

        String baseUrl = imageStorageProperties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return url;
        }

        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (!url.startsWith("/")) {
            return normalizedBase + "/" + url;
        }

        URI baseUri = URI.create(normalizedBase);
        String origin = baseUri.getScheme() + "://" + baseUri.getAuthority();
        return origin + url;
    }
}
