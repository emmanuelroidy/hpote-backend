package com.hpote.backend.assistant.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hpote.backend.assistant.dto.AiHelpView;
import com.hpote.backend.common.ApiExceptionDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiHelpService {

    private static final Logger log = LoggerFactory.getLogger(AiHelpService.class);
    private static final Map<String, String> TOPICS = Map.of(
            "CREATE_REQUEST", "Create a service request: choose Find, select Ask for help, choose a service, describe the job, confirm the service area, and submit. Photos and community sharing are optional.",
            "FIND_PROFESSIONAL", "Find a professional: open Find, choose Professionals, review nearby profiles, ratings, and distance, then select a professional to view their details.",
            "TRACK_REQUEST", "Track a request: open Requests and choose Active, Completed, or Cancelled. Open a request to see its status and any responses.",
            "COMPARE_QUOTES", "Compare responses: open a request, read each professional's offer and details, and accept the response that best suits the customer. Do not claim the app verifies prices or guarantees work.",
            "MESSAGE_PROFESSIONAL", "Message a professional: open an accepted or in-progress request and use its message action to coordinate service details.",
            "PRO_FIND_WORK", "For professionals: open Find to see requests that match the professions and service area on the profile. Open a request to review it and respond.",
            "PRO_RESPOND", "For professionals: open Requests, select a nearby request, review the details, and submit a clear response. Keep communication about the job in its request conversation.");

    private final GeminiClient geminiClient;

    public AiHelpView answer(String topic, String languageCode) {
        String topicGuidance = topic == null ? null : TOPICS.get(topic);
        if (topicGuidance == null) {
            throw new ApiExceptionDto("Choose one of the available help topics");
        }
        String prompt = """
                You are HpOtE's in-app guide for customers and local service professionals.
                Explain only how to use the app for the selected topic below. Do not invent features,
                service terms, prices, guarantees, or professional advice. Use 2 or 3 short, friendly
                sentences in the language identified by this language tag: %s.
                Selected app help topic: %s
                """.formatted(languageCode, topicGuidance);
        try {
            return new AiHelpView(geminiClient.generate(prompt, 180));
        } catch (ApiExceptionDto exception) {
            log.warn("Gemini help unavailable for topic {}", topic);
            throw new ApiExceptionDto("AI help is temporarily unavailable. Please try again.");
        }
    }
}
