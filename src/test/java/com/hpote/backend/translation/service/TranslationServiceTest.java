package com.hpote.backend.translation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hpote.backend.translation.model.TranslationEntry;
import com.hpote.backend.translation.repository.TranslationEntryRepository;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock
    private TranslationEntryRepository translationRepository;

    @InjectMocks
    private TranslationService translationService;

    @Test
    void adminDictionaryEntryTakesPrecedenceOverAutomaticTranslation() {
        TranslationEntry automatic = entry("auto.key", "Habari", true);
        TranslationEntry admin = entry("greeting", "Shikamoo", false);
        when(translationRepository.findByTargetLanguageOrderByTranslationKeyAsc("sw"))
                .thenReturn(List.of(automatic, admin));

        assertEquals(Map.of("Hello", "Shikamoo"), translationService.dictionary("sw"));
    }

    @Test
    void resolveUsesExistingDictionaryEntryWithoutCallingTranslator() {
        TranslationEntry automatic = entry("auto.key", "Habari", true);
        TranslationEntry admin = entry("greeting", "Shikamoo", false);
        when(translationRepository.findByTargetLanguageAndSourceTextIn("sw", List.of("Hello")))
                .thenReturn(List.of(automatic, admin));

        assertEquals(Map.of("Hello", "Shikamoo"),
                translationService.resolve("sw", List.of("Hello")));
    }

    private TranslationEntry entry(String key, String translated, boolean automatic) {
        TranslationEntry entry = new TranslationEntry();
        entry.setTranslationKey(key);
        entry.setTargetLanguage("sw");
        entry.setSourceText("Hello");
        entry.setTranslatedText(translated);
        entry.setAutomaticallyTranslated(automatic);
        return entry;
    }
}
