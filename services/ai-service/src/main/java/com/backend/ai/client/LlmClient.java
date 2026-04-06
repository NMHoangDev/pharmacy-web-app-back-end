package com.backend.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class LlmClient {

    private final RestClient restClient;
    private final boolean enabled;
    private final String provider;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final List<String> fallbackModels;

    public LlmClient(RestClient.Builder builder,
            @Value("${ai.llm.enabled:false}") boolean enabled,
            @Value("${ai.llm.provider:openai}") String provider,
            @Value("${ai.llm.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ai.llm.api-key:}") String apiKey,
            @Value("${ai.llm.model:gpt-4.1-mini}") String model,
            @Value("${ai.llm.fallback-models:}") String fallbackModels) {
        this.restClient = builder.build();
        this.enabled = enabled;
        this.provider = provider;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.fallbackModels = parseFallbackModels(fallbackModels);
    }

    public boolean isReady() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public String model() {
        return model;
    }

    public String chat(String systemPrompt, List<Message> messages) {
        return chat(systemPrompt, messages, LlmOptions.defaults());
    }

    public String chatJson(String systemPrompt, List<Message> messages, int maxOutputTokens) {
        return chat(systemPrompt, messages, LlmOptions.forJson(maxOutputTokens));
    }

    public String chat(String systemPrompt, List<Message> messages, LlmOptions options) {
        LlmOptions safeOptions = options == null ? LlmOptions.defaults() : options;
        String normalizedProvider = provider == null ? "openai" : provider.trim().toLowerCase(Locale.ROOT);
        if ("gemini".equals(normalizedProvider)) {
            return chatWithGemini(systemPrompt, messages, safeOptions);
        }
        return chatWithOpenAiCompatible(systemPrompt, messages, safeOptions);
    }

    private String chatWithOpenAiCompatible(String systemPrompt, List<Message> messages, LlmOptions options) {
        List<Message> merged = new ArrayList<>();
        merged.add(new Message("system", systemPrompt));
        merged.addAll(messages);

        ChatCompletionRequest request = new ChatCompletionRequest(model, merged, options.temperature());
        ChatCompletionResponse response = restClient.post()
                .uri(baseUrl + "/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(request)
                .retrieve()
                .body(ChatCompletionResponse.class);
        if (response == null || response.choices == null || response.choices.isEmpty()) {
            return "";
        }
        ChatChoice choice = response.choices.get(0);
        return choice.message == null || choice.message.content == null ? "" : choice.message.content.trim();
    }

    private String chatWithGemini(String systemPrompt, List<Message> messages, LlmOptions options) {
        List<String> modelCandidates = new ArrayList<>();
        if (model != null && !model.isBlank()) {
            modelCandidates.add(model.trim());
        }
        for (String fallbackModel : fallbackModels) {
            if (!modelCandidates.contains(fallbackModel)) {
                modelCandidates.add(fallbackModel);
            }
        }

        RuntimeException lastFailure = null;
        for (String candidateModel : modelCandidates) {
            try {
                String reply = chatWithGeminiModel(candidateModel, systemPrompt, messages, options);
                if (!reply.isBlank()) {
                    return reply;
                }
            } catch (RestClientResponseException ex) {
                if (shouldTryNextGeminiModel(ex)) {
                    lastFailure = ex;
                    continue;
                }
                throw ex;
            } catch (RuntimeException ex) {
                lastFailure = ex;
                break;
            }
        }

        if (lastFailure != null) {
            throw lastFailure;
        }
        return "";
    }

        private String chatWithGeminiModel(String targetModel, String systemPrompt, List<Message> messages, LlmOptions options) {
        GeminiGenerateContentRequest request = new GeminiGenerateContentRequest(
                new GeminiSystemInstruction(List.of(new GeminiPart(systemPrompt))),
                messages.stream()
                        .map(message -> new GeminiContent(mapGeminiRole(message.role()), List.of(new GeminiPart(message.content()))))
                        .toList(),
            new GeminiGenerationConfig(
                options.temperature(),
                options.maxOutputTokens(),
                options.responseMimeType()));

        GeminiGenerateContentResponse response = restClient.post()
                .uri(baseUrl + "/models/" + targetModel + ":generateContent?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GeminiGenerateContentResponse.class);

        if (response == null || response.candidates == null || response.candidates.isEmpty()) {
            return "";
        }
        GeminiCandidate candidate = response.candidates.get(0);
        if (candidate.content == null || candidate.content.parts == null || candidate.content.parts.isEmpty()) {
            return "";
        }
        return candidate.content.parts.stream()
                .map(part -> part.text == null ? "" : part.text)
                .reduce("", String::concat)
                .trim();
    }

    private List<String> parseFallbackModels(String rawFallbackModels) {
        if (rawFallbackModels == null || rawFallbackModels.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rawFallbackModels.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private boolean shouldTryNextGeminiModel(RestClientResponseException ex) {
        if (ex.getStatusCode().value() == 429) {
            return true;
        }
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return false;
        }
        String normalized = body.toLowerCase(Locale.ROOT);
        return normalized.contains("resource_exhausted")
                || normalized.contains("quota exceeded")
                || normalized.contains("rate limit");
    }

    private String mapGeminiRole(String role) {
        if (role == null) {
            return "user";
        }
        return switch (role.trim().toLowerCase(Locale.ROOT)) {
            case "assistant", "model" -> "model";
            default -> "user";
        };
    }

    public record Message(String role, String content) {
    }

    public record ChatCompletionRequest(String model, List<Message> messages, Double temperature) {
    }

    public record GeminiGenerateContentRequest(
            GeminiSystemInstruction systemInstruction,
            List<GeminiContent> contents,
            GeminiGenerationConfig generationConfig) {
    }

    public record GeminiSystemInstruction(List<GeminiPart> parts) {
    }

    public record GeminiContent(String role, List<GeminiPart> parts) {
    }

    public record GeminiPart(String text) {
    }

    public record GeminiGenerationConfig(Double temperature, Integer maxOutputTokens, String responseMimeType) {
    }

    public record LlmOptions(Double temperature, Integer maxOutputTokens, String responseMimeType) {
        public static LlmOptions defaults() {
            return new LlmOptions(0.2, 384, null);
        }

        public static LlmOptions forJson(int maxOutputTokens) {
            int safeMaxTokens = Math.max(256, maxOutputTokens);
            return new LlmOptions(0.1, safeMaxTokens, "application/json");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatCompletionResponse {
        public List<ChatChoice> choices;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatChoice {
        public ChatChoiceMessage message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatChoiceMessage {
        public String role;
        public String content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiGenerateContentResponse {
        public List<GeminiCandidate> candidates;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiCandidate {
        public GeminiContentResponse content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiContentResponse {
        public List<GeminiPartResponse> parts;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiPartResponse {
        public String text;
    }
}
