package com.hpote.backend.media.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hpote.backend.media.model.StoredImageEntity;

public interface StoredImageRepository extends JpaRepository<StoredImageEntity, String> {
}
