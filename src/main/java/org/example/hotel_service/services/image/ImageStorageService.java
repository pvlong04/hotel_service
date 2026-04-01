package org.example.hotel_service.services.image;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.config.ImageStorageProperties;
import org.example.hotel_service.dtos.response.ImageUploadResponse;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ImageStorageService {

    ImageStorageProperties imageStorageProperties;

    public ImageUploadResponse store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        try {
            Path uploadDir = Paths.get(imageStorageProperties.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            String originalName = Objects.requireNonNullElse(file.getOriginalFilename(), "image");
            String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            String storedName = UUID.randomUUID() + "_" + safeName;

            Path target = uploadDir.resolve(storedName).normalize();
            if (!target.startsWith(uploadDir)) {
                throw new ApiException(ErrorCode.INVALID_REQUEST);
            }

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String baseUrl = imageStorageProperties.getBaseUrl();
            String publicUrl = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + storedName;

            return ImageUploadResponse.builder()
                    .fileName(storedName)
                    .url(publicUrl)
                    .size(file.getSize())
                    .contentType(contentType)
                    .build();
        } catch (IOException ex) {
            throw new ApiException(ErrorCode.RUNTIME_ERROR);
        }
    }
}

