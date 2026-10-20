package com.hpote.backend.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;

import com.hpote.backend.common.ApiExceptionDto;
import com.hpote.backend.media.service.MediaStorageService;

class MediaStorageServiceTest {

    private Path uploadDirectory;
    private MediaStorageService storage;

    @BeforeEach
    void setUp() {
        uploadDirectory = Path.of("target", "test-uploads-" + UUID.randomUUID()).toAbsolutePath();
        storage = new MediaStorageService(uploadDirectory.toString(), "");
    }

    @AfterEach
    void cleanUp() throws Exception {
        if (Files.exists(uploadDirectory)) {
            try (var files = Files.walk(uploadDirectory)) {
                files.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                });
            }
        }
    }

    @Test
    void storesValidatedPngAndReturnsResolvableApiPath() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB), "png", bytes);
        MockMultipartFile upload = new MockMultipartFile("file", "customer-photo.png",
                MediaType.IMAGE_PNG_VALUE, bytes.toByteArray());

        String url = storage.storeImage(upload);

        assertTrue(url.matches("/api/media/images/[0-9a-f-]{36}"));
        MediaStorageService.StoredImage result =
                storage.loadImage(url.substring(url.lastIndexOf('/') + 1));
        assertEquals(MediaType.IMAGE_PNG, result.contentType());
        assertTrue(result.resource().exists());
    }

    @Test
    void rejectsNonImageAndMismatchedMimeTypes() throws Exception {
        MockMultipartFile text = new MockMultipartFile("file", "photo.png",
                MediaType.IMAGE_PNG_VALUE, "not an image".getBytes());
        MockMultipartFile unsupported = new MockMultipartFile("file", "photo.svg",
                "image/svg+xml", "<svg/>".getBytes());
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB), "png", png);
        MockMultipartFile mismatchedMime = new MockMultipartFile("file", "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE, png.toByteArray());

        assertThrows(ApiExceptionDto.class, () -> storage.storeImage(text));
        assertThrows(ApiExceptionDto.class, () -> storage.storeImage(unsupported));
        assertThrows(ApiExceptionDto.class, () -> storage.storeImage(mismatchedMime));
    }
}
