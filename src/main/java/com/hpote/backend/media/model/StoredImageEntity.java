package com.hpote.backend.media.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "media_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoredImageEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 32)
    private String contentType;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false, length = 5 * 1024 * 1024)
    private byte[] content;

    public StoredImageEntity(String id, String contentType, byte[] content) {
        this.id = id;
        this.contentType = contentType;
        this.content = content;
    }
}
