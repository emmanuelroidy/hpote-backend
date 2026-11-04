package com.hpote.backend.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.hpote.backend.assistant.service.AiHelpService;
import com.hpote.backend.assistant.service.GeminiClient;
import com.hpote.backend.common.ApiExceptionDto;

class AiHelpServiceTest {

    @Test
    void reportsWhenGeminiIsNotConfigured() {
        GeminiClient geminiClient = mock(GeminiClient.class);
        when(geminiClient.generate(anyString(), eq(180)))
                .thenThrow(new ApiExceptionDto("The Gemini API key is not configured"));
        AiHelpService service = new AiHelpService(geminiClient);

        ApiExceptionDto exception = assertThrows(
                ApiExceptionDto.class,
                () -> service.answer("CREATE_REQUEST", "en"));

        assertEquals("AI help is temporarily unavailable. Please try again.", exception.getMessage());
    }

    @Test
    void rejectsTopicsOutsideThePredefinedHelpList() {
        GeminiClient geminiClient = mock(GeminiClient.class);
        AiHelpService service = new AiHelpService(geminiClient);

        ApiExceptionDto exception = assertThrows(
                ApiExceptionDto.class,
                () -> service.answer("SEND_PRIVATE_MESSAGE", "en"));

        assertEquals("Choose one of the available help topics", exception.getMessage());
        verify(geminiClient, org.mockito.Mockito.never()).generate(anyString(), eq(180));
    }
}
