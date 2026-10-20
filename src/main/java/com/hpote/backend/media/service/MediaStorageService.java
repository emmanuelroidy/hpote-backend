package com.hpote.backend.media.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.hpote.backend.common.ApiExceptionDto;

@Service
public class MediaStorageService {

    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private final Path uploadDirectory;
    private final String publicBaseUrl;

    public MediaStorageService(
            @Value("${app.media.upload-dir:uploads/customer-requests}") String uploadDirectory,
            @Value("${app.media.public-base-url:}") String publicBaseUrl) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/+$", "");
    }

    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiExceptionDto("An image file is required");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new ApiExceptionDto("Image must be 5 MB or smaller");
        }
        String contentType = file.getContentType();
        String extension = switch (contentType == null ? "" : contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> throw new ApiExceptionDto("Only JPEG and PNG images are supported");
        };

        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES || !isValidImage(bytes, extension)) {
                throw new ApiExceptionDto("Uploaded file is not a valid image");
            }
            Files.createDirectories(uploadDirectory);
            String id = UUID.randomUUID().toString();
            Path destination = uploadDirectory.resolve(id + extension).normalize();
            if (!destination.startsWith(uploadDirectory)) {
                throw new ApiExceptionDto("Invalid upload path");
            }
            Files.write(destination, bytes);
            String relativeUrl = "/api/media/images/" + id;
            return publicBaseUrl.isEmpty() ? relativeUrl : publicBaseUrl + relativeUrl;
        } catch (IOException exception) {
            throw new ApiExceptionDto("Unable to store image");
        }
    }

    private boolean isValidImage(byte[] bytes, String expectedExtension) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                return false;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return false;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                boolean matchingFormat = expectedExtension.equals(".jpg")
                        ? format.equals("jpeg") || format.equals("jpg")
                        : format.equals("png");
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                return matchingFormat && width > 0 && height > 0
                        && width <= 8000 && height <= 8000 && (long) width * height <= 25_000_000L;
            } finally {
                reader.dispose();
            }
        }
    }

    public StoredImage loadImage(String rawId) {
        final UUID id;
        try {
            id = UUID.fromString(rawId);
            if (!id.toString().equalsIgnoreCase(rawId)) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ApiExceptionDto("Image not found");
        }
        for (String extension : new String[] {".jpg", ".png"}) {
            Path file = uploadDirectory.resolve(id + extension).normalize();
            if (file.startsWith(uploadDirectory) && Files.isRegularFile(file)) {
                try {
                    Resource resource = new UrlResource(file.toUri());
                    MediaType type = extension.equals(".jpg") ? MediaType.IMAGE_JPEG : MediaType.IMAGE_PNG;
                    return new StoredImage(resource, type);
                } catch (IOException exception) {
                    throw new ApiExceptionDto("Unable to read image");
                }
            }
        }
        throw new ApiExceptionDto("Image not found");
    }

    public record StoredImage(Resource resource, MediaType contentType) {
    }
}
