package com.hpote.backend.translation.model;

import com.hpote.backend.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "translation_entries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_translation_key_language", columnNames = { "translationKey", "targetLanguage" })
})
@Getter
@Setter
@NoArgsConstructor
public class TranslationEntry extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String translationKey;

    @Column(nullable = false, length = 12)
    private String sourceLanguage;

    @Column(nullable = false, length = 12)
    private String targetLanguage;

    @Column(nullable = false, length = 2000)
    private String sourceText;

    @Column(nullable = false, length = 2000)
    private String translatedText;

    @Column(nullable = false)
    private boolean automaticallyTranslated;
}
