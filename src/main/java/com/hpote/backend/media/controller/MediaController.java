package com.hpote.backend.media.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.hpote.backend.common.ApiResponseDto;
import com.hpote.backend.media.dto.MediaUploadView;
import com.hpote.backend.media.service.MediaStorageService;
import com.hpote.backend.media.service.MediaStorageService.StoredImage;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/media/images")
@RequiredArgsConstructor
public class MediaController {

    private final MediaStorageService mediaStorageService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<MediaUploadView>> upload(
            @RequestPart("file") MultipartFile file) {
        String url = mediaStorageService.storeImage(file);
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Image uploaded",
                new MediaUploadView(url)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getImage(@PathVariable String id) {
        StoredImage image = mediaStorageService.loadImage(id);
        return ResponseEntity.ok()
                .contentType(image.contentType())
                .cacheControl(CacheControl.noCache().cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.resource());
    }
}
