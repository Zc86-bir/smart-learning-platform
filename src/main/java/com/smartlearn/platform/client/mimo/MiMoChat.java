package com.smartlearn.platform.client.mimo;

import java.util.List;

/**
 * MiMo Chat Completion API request/response records.
 * Supports text-only and multimodal vision messages.
 */
public final class MiMoChat {

    private MiMoChat() {}

    public record Request(
        String model,
        List<Message> messages,
        Double temperature,
        Thinking thinking,
        ResponseFormat response_format,
        Integer max_tokens
    ) {
        public Request withMaxTokens(int max) {
            return new Request(model, messages, temperature, thinking, response_format, max);
        }
    }

    /**
     * A chat message. Use {@code content(String)} for text-only,
     * or {@code content(List<ContentPart>)} for vision (text + images).
     */
    public record Message(
        String role,
        Object content
    ) {
        public static Message system(String content) {
            return new Message("system", content);
        }

        public static Message user(String content) {
            return new Message("user", content);
        }

        public static Message assistant(String content) {
            return new Message("assistant", content);
        }

        /**
         * Multi-modal message: text + one or more image parts.
         */
        public static Message userWithImages(String text, List<ContentPart> imageParts) {
            var parts = new java.util.ArrayList<ContentPart>();
            parts.add(new ContentPart("text", text, null));
            parts.addAll(imageParts);
            return new Message("user", parts);
        }
    }

    /**
     * A single content part within a multi-modal message.
     */
    public record ContentPart(
        String type,
        String text,
        ImageUrl image_url
    ) {
        public static ContentPart text(String text) {
            return new ContentPart("text", text, null);
        }

        public static ContentPart imageUrl(String base64DataUri, String detail) {
            return new ContentPart("image_url", null, new ImageUrl(base64DataUri, detail));
        }
    }

    public record ImageUrl(
        String url,
        String detail
    ) {}

    public record Thinking(
        String type
    ) {}

    public record ResponseFormat(
        String type
    ) {}

    public record Response(
        List<Choice> choices,
        Usage usage
    ) {
        public String firstContent() {
            if (choices != null && !choices.isEmpty()) {
                var msg = choices.getFirst().message();
                if (msg == null) return null;
                // Text-only message
                if (msg.content instanceof String s) return s;
                // Multi-modal: extract text parts
                if (msg.content instanceof List<?> parts) {
                    return parts.stream()
                        .filter(p -> p instanceof ContentPart cp && "text".equals(cp.type))
                        .map(p -> ((ContentPart) p).text())
                        .collect(java.util.stream.Collectors.joining("\n"));
                }
                return msg.content != null ? msg.content.toString() : null;
            }
            return null;
        }

        public int promptTokens() {
            return usage != null ? usage.promptTokens() : 0;
        }

        public int completionTokens() {
            return usage != null ? usage.completionTokens() : 0;
        }

        public int totalTokens() {
            return usage != null ? usage.totalTokens() : 0;
        }
    }

    public record Choice(
        Message message,
        String finish_reason
    ) {}

    public record Usage(
        int prompt_tokens,
        int completion_tokens,
        int total_tokens
    ) {
        public int promptTokens() { return prompt_tokens; }
        public int completionTokens() { return completion_tokens; }
        public int totalTokens() { return total_tokens; }
    }

    public record StreamChunk(
        List<StreamChoice> choices
    ) {
        public String content() {
            if (choices != null && !choices.isEmpty()) {
                var delta = choices.getFirst().delta();
                return delta != null ? delta.content() : null;
            }
            return null;
        }

        public String finishReason() {
            if (choices != null && !choices.isEmpty()) {
                return choices.getFirst().finish_reason();
            }
            return null;
        }
    }

    public record StreamChoice(
        StreamDelta delta,
        String finish_reason
    ) {}

    public record StreamDelta(
        String content,
        String role
    ) {}
}
