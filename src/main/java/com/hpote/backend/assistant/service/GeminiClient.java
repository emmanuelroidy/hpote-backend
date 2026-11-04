package com.hpote.backend.assistant.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.hpote.backend.common.ApiExceptionDto;

@Service
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final RestClient restClient;
    private final String model;
    private final String apiKey;
    private final String baseUrl;

    public GeminiClient(
            @Value("${app.ai.gemini-model:${GEMINI_MODEL:gemini-3.5-flash-lite}}") String model,
            @Value("${app.ai.gemini-api-key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${app.ai.gemini-base-url:https://generativelanguage.googleapis.com/v1beta/models/}") String baseUrl) {
        this.model = model;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(12));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public String generate(String prompt, int maxOutputTokens) {
        if (apiKey.isBlank()) {
            throw new ApiExceptionDto("The Gemini API key is not configured");
        }
        Map<String, Object> request = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", maxOutputTokens));
        try {
            Map<String, Object> response = restClient.post()
                    .uri(baseUrl + model + ":generateContent")
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            String generatedText = responseText(response);
            if (generatedText == null || generatedText.isBlank()) {
                log.warn("Gemini returned no generated text");
                throw new ApiExceptionDto("Gemini did not return generated text");
            }
            return generatedText.trim();
        } catch (RestClientException exception) {
            log.warn("Gemini request failed ({})", exception.getClass().getSimpleName());
            throw new ApiExceptionDto("Gemini is temporarily unavailable");
        }
    }

    private String responseText(Map<String, Object> response) {
        Object candidatesValue = response == null ? null : response.get("candidates");
        if (!(candidatesValue instanceof List<?> candidates) || candidates.isEmpty()
                || !(candidates.get(0) instanceof Map<?, ?> candidate)
                || !(candidate.get("content") instanceof Map<?, ?> content)
                || !(content.get("parts") instanceof List<?> parts)
                || parts.isEmpty()
                || !(parts.get(0) instanceof Map<?, ?> part)
                || !(part.get("text") instanceof String text)) {
            return null;
        }
        return text;
    }
}
