package com.hpote.backend.translation.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hpote.backend.translation.model.TranslationEntry;

public interface TranslationEntryRepository extends JpaRepository<TranslationEntry, String> {

    List<TranslationEntry> findByTargetLanguageOrderByTranslationKeyAsc(String targetLanguage);

    List<TranslationEntry> findByTargetLanguageAndSourceTextIn(String targetLanguage,
            Collection<String> sourceTexts);

    Optional<TranslationEntry> findByTranslationKeyAndTargetLanguage(String translationKey, String targetLanguage);

    Optional<TranslationEntry> findByIdAndTargetLanguage(String id, String targetLanguage);

    @Query("select distinct entry.targetLanguage from TranslationEntry entry")
    List<String> findDistinctTargetLanguages();
}
