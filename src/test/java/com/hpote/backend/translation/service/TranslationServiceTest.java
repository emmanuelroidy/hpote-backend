package com.hpote.backend.translation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.hpote.backend.assistant.service.AiTranslationService;
import com.hpote.backend.common.ApiExceptionDto;
import com.hpote.backend.translation.model.TranslationEntry;
import com.hpote.backend.translation.repository.TranslationEntryRepository;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock
    private TranslationEntryRepository translationRepository;

    @Mock
    private AiTranslationService aiTranslationService;

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

    @Test
    void resolveWithAiStillPrefersExistingDictionaryEntries() {
        TranslationEntry automatic = entry("auto.key", "Habari", true);
        TranslationEntry admin = entry("greeting", "Shikamoo", false);
        when(translationRepository.findByTargetLanguageAndSourceTextIn("sw", List.of("Hello")))
                .thenReturn(List.of(automatic, admin));

        assertEquals(Map.of("Hello", "Shikamoo"),
                translationService.resolve("sw", List.of("Hello"), true));

        verifyNoInteractions(aiTranslationService);
    }

    @Test
    void resolveWithAiUsesGeminiForMissingUiLabels() {
        when(translationRepository.findByTargetLanguageAndSourceTextIn("sw", List.of("Hello")))
                .thenReturn(List.of());
        when(translationRepository.findByTranslationKeyAndTargetLanguage(anyString(), eq("sw")))
                .thenReturn(Optional.empty());
        when(aiTranslationService.translateUiLabels(List.of("Hello"), "sw"))
                .thenReturn(List.of("Habari"));

        assertEquals(Map.of("Hello", "Habari"),
                translationService.resolve("sw", List.of("Hello"), true));

        verify(aiTranslationService).translateUiLabels(List.of("Hello"), "sw");
    }

    @Test
    void resolveWithAiFallsBackToLibreTranslateWhenGeminiFails() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/translate", exchange -> {
            byte[] response = "{\"translatedText\":\"Habari\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(
                    translationService,
                    "libreTranslateUrl",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/translate");
            ReflectionTestUtils.setField(translationService, "libreTranslateApiKey", "");
            when(translationRepository.findByTargetLanguageAndSourceTextIn("sw", List.of("Hello")))
                    .thenReturn(List.of());
            when(translationRepository.findByTranslationKeyAndTargetLanguage(anyString(), eq("sw")))
                    .thenReturn(Optional.empty());
            when(aiTranslationService.translateUiLabels(List.of("Hello"), "sw"))
                    .thenThrow(new ApiExceptionDto("Gemini is unavailable"));

            assertEquals(Map.of("Hello", "Habari"),
                    translationService.resolve("sw", List.of("Hello"), true));
        } finally {
            server.stop(0);
        }
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
