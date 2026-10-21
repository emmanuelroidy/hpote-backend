package com.hpote.backend.translation.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HexFormat;
import java.net.http.HttpClient;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.hpote.backend.common.ApiExceptionDto;
import com.hpote.backend.translation.dto.TranslationEntryRequest;
import com.hpote.backend.translation.model.TranslationEntry;
import com.hpote.backend.translation.repository.TranslationEntryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);
    private static final String SOURCE_LANGUAGE = "en";

    private final TranslationEntryRepository translationRepository;

    @Value("${app.translation.libretranslate-url:${LIBRETRANSLATE_URL:http://localhost:5001/translate}}")
    private String libreTranslateUrl;

    @Value("${app.translation.api-key:${LIBRETRANSLATE_API_KEY:}}")
    private String libreTranslateApiKey;

    @Transactional(readOnly = true)
    public List<TranslationEntry> entries(String rawLanguageCode) {
        return translationRepository.findByTargetLanguageOrderByTranslationKeyAsc(normalizeLanguage(rawLanguageCode));
    }

    @Transactional
    public TranslationEntry create(TranslationEntryRequest request) {
        String language = normalizeLanguage(request.targetLanguage());
        String key = request.translationKey().trim();
        if (translationRepository.findByTranslationKeyAndTargetLanguage(key, language).isPresent()) {
            throw new ApiExceptionDto("A translation with this key already exists for that language");
        }
        TranslationEntry entry = new TranslationEntry();
        apply(entry, request);
        entry.setAutomaticallyTranslated(false);
        return translationRepository.save(entry);
    }

    @Transactional
    public TranslationEntry update(String id, TranslationEntryRequest request) {
        String language = normalizeLanguage(request.targetLanguage());
        TranslationEntry entry = translationRepository.findByIdAndTargetLanguage(id, language)
                .orElseThrow(() -> new ApiExceptionDto("Translation not found"));
        translationRepository.findByTranslationKeyAndTargetLanguage(request.translationKey().trim(), language)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ApiExceptionDto("A translation with this key already exists for that language");
                });
        apply(entry, request);
        entry.setAutomaticallyTranslated(false);
        return translationRepository.save(entry);
    }

    @Transactional
    public void delete(String id) {
        TranslationEntry entry = translationRepository.findById(id)
                .orElseThrow(() -> new ApiExceptionDto("Translation not found"));
        translationRepository.delete(entry);
    }

    @Transactional(readOnly = true)
    public Map<String, String> dictionary(String rawLanguageCode) {
        String language = normalizeLanguage(rawLanguageCode);
        Map<String, String> dictionary = new LinkedHashMap<>();
        for (TranslationEntry entry : translationRepository.findByTargetLanguageOrderByTranslationKeyAsc(language)
                .stream()
                .sorted(Comparator.comparing(TranslationEntry::isAutomaticallyTranslated).reversed())
                .toList()) {
            dictionary.put(entry.getSourceText(), entry.getTranslatedText());
        }
        return dictionary;
    }

    @Transactional
    public Map<String, String> resolve(String rawLanguageCode, List<String> rawTexts) {
        String language = normalizeLanguage(rawLanguageCode);
        List<String> texts = rawTexts.stream().distinct().toList();
        if (SOURCE_LANGUAGE.equals(language)) {
            return texts.stream().collect(LinkedHashMap::new, (map, text) -> map.put(text, text),
                    LinkedHashMap::putAll);
        }

        Map<String, String> translations = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();
        translationRepository.findByTargetLanguageAndSourceTextIn(language, texts).stream()
                .sorted(Comparator.comparing(TranslationEntry::isAutomaticallyTranslated))
                .forEach(entry -> translations.putIfAbsent(entry.getSourceText(), entry.getTranslatedText()));
        for (String text : texts) {
            if (!translations.containsKey(text)) {
                missing.add(text);
            }
        }

        if (!missing.isEmpty()) {
            List<String> generated = translateWithLibreTranslate(missing, language);
            for (int index = 0; index < missing.size(); index++) {
                String sourceText = missing.get(index);
                String translatedText = index < generated.size() ? generated.get(index) : sourceText;
                if (translatedText == null || translatedText.isBlank()) {
                    translatedText = sourceText;
                }
                translations.put(sourceText, translatedText);
                saveAutomaticTranslation(sourceText, language, translatedText);
            }
        }
        return translations;
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> languages() {
        Map<String, String> languages = new LinkedHashMap<>();
        languages.put("en", "English");
        languages.put("sw", "Swahili");
        translationRepository.findDistinctTargetLanguages()
                .forEach(code -> languages.putIfAbsent(code, code.toUpperCase(Locale.ROOT)));
        return languages.entrySet().stream()
                .map(entry -> Map.of("code", entry.getKey(), "name", entry.getValue()))
                .toList();
    }

    private List<String> translateWithLibreTranslate(List<String> texts, String targetLanguage) {
        List<String> translatedTexts = new ArrayList<>();
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        RestClient client = RestClient.builder().requestFactory(requestFactory).build();
        for (String text : texts) {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("q", text);
            request.put("source", SOURCE_LANGUAGE);
            request.put("target", targetLanguage.split("-")[0]);
            request.put("format", "text");
            if (!libreTranslateApiKey.isBlank()) {
                request.put("api_key", libreTranslateApiKey);
            }

            try {
                LibreTranslateResponse response = client
                    .post()
                    .uri(libreTranslateUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(LibreTranslateResponse.class);
                if (response == null) {
                    throw new ApiExceptionDto("The automatic translator returned an empty response");
                }
                if (response.translatedText() == null || response.translatedText().isBlank()) {
                    throw new ApiExceptionDto("The automatic translator returned an invalid translation");
                }
                translatedTexts.add(response.translatedText());
            } catch (RestClientException exception) {
                log.warn("LibreTranslate fallback unavailable: {}", exception.getMessage());
                throw new ApiExceptionDto(
                        "Automatic translation is unavailable. Check the configured LibreTranslate service.");
            }
        }
        return translatedTexts;
    }

    private void saveAutomaticTranslation(String sourceText, String targetLanguage, String translatedText) {
        String key = "auto." + digest(targetLanguage + "\n" + sourceText);
        TranslationEntry entry = translationRepository
                .findByTranslationKeyAndTargetLanguage(key, targetLanguage)
                .orElseGet(TranslationEntry::new);
        entry.setTranslationKey(key);
        entry.setSourceLanguage(SOURCE_LANGUAGE);
        entry.setTargetLanguage(targetLanguage);
        entry.setSourceText(sourceText);
        entry.setTranslatedText(translatedText);
        entry.setAutomaticallyTranslated(true);
        translationRepository.save(entry);
    }

    private void apply(TranslationEntry entry, TranslationEntryRequest request) {
        entry.setTranslationKey(request.translationKey().trim());
        entry.setSourceLanguage(normalizeLanguage(request.sourceLanguage()));
        entry.setTargetLanguage(normalizeLanguage(request.targetLanguage()));
        entry.setSourceText(request.sourceText().trim());
        entry.setTranslatedText(request.translatedText().trim());
    }

    private String normalizeLanguage(String language) {
        if (language == null || !language.matches("^[A-Za-z]{2,3}([_-][A-Za-z0-9]{2,8})*$")) {
            throw new ApiExceptionDto("Language code must be a valid language tag");
        }
        return language.replace('_', '-').toLowerCase(Locale.ROOT);
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record LibreTranslateResponse(String translatedText) {
    }
}
