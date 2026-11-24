package com.nova.bank.services;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@Service
public class CustomerImageStorageService {

    @Value("${nova.upload.customer-dir}")
    private String uploadDirectory;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    public String storeImage(MultipartFile image, String customerId) {

        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Customer image is required");
        }

        if (image.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image size must not exceed 5 MB");
        }

        String extension = ALLOWED_TYPES.get(image.getContentType());

        if (extension == null) {
            throw new IllegalArgumentException(
                    "Only JPG, PNG and WEBP images are allowed"
            );
        }

        try {
            Path directory = Paths.get(uploadDirectory)
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(directory);

            String fileName = customerId + "_" + UUID.randomUUID() + extension;

            Path filePath = directory.resolve(fileName).normalize();

            if (!filePath.startsWith(directory)) {
                throw new IllegalArgumentException("Invalid file path");
            }

            Files.copy(image.getInputStream(), filePath);

            return fileName;

        } catch (IOException ex) {
            throw new RuntimeException("Failed to store customer image", ex);
        }
    }
}