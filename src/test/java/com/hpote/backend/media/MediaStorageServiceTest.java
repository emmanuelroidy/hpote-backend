package com.hpote.backend.media;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import com.hpote.backend.common.ApiExceptionDto;
import com.hpote.backend.media.model.StoredImageEntity;
import com.hpote.backend.media.repository.StoredImageRepository;
import com.hpote.backend.media.service.MediaStorageService;

class MediaStorageServiceTest {

    @Test
    void storesValidatedPngInDatabaseAndReturnsResolvableApiPath() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB), "png", bytes);
        MockMultipartFile upload = new MockMultipartFile("file", "customer-photo.png",
                MediaType.IMAGE_PNG_VALUE, bytes.toByteArray());
        StoredImageRepository repository = mock(StoredImageRepository.class);
        when(repository.save(any(StoredImageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return Optional.of(
                    new StoredImageEntity(id, MediaType.IMAGE_PNG_VALUE, bytes.toByteArray()));
        });
        MediaStorageService storage = new MediaStorageService(repository, "target/test-uploads", "");

        String url = storage.storeImage(upload);

        assertTrue(url.matches("/api/media/images/[0-9a-f-]{36}"));
        ArgumentCaptor<StoredImageEntity> storedImage = ArgumentCaptor.forClass(StoredImageEntity.class);
        verify(repository).save(storedImage.capture());
        assertEquals(MediaType.IMAGE_PNG_VALUE, storedImage.getValue().getContentType());
        assertArrayEquals(bytes.toByteArray(), storedImage.getValue().getContent());
        MediaStorageService.StoredImage result =
                storage.loadImage(url.substring(url.lastIndexOf('/') + 1));
        assertEquals(MediaType.IMAGE_PNG, result.contentType());
        assertTrue(result.resource().exists());
        assertEquals(bytes.size(), result.resource().contentLength());
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
        MediaStorageService storage = new MediaStorageService(
                mock(StoredImageRepository.class), "target/test-uploads", "");

        assertThrows(ApiExceptionDto.class, () -> storage.storeImage(text));
        assertThrows(ApiExceptionDto.class, () -> storage.storeImage(unsupported));
        assertThrows(ApiExceptionDto.class, () -> storage.storeImage(mismatchedMime));
    }
}
