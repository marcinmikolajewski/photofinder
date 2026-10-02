package eu.mm.software.photofinder.photosattribute.domain;

import lombok.Getter;

@Getter
public enum AiProvider {

    OLLAMA(false, true, 336),        // LLaVA 1.5 - fixed 336×336 internal
    GROQ(false, false, 1024),        // Llama 4 Scout
    MISTRAL(false, true, 1024),      // Pixtral - multi-tile 1024px
    NVIDIA(false, true, 768),        // Phi-4 multimodal
    CLOUDFLARE(false, false, 336),   // LLaVA 1.5-7b - fixed 336×336 internal
    CEREBRAS(false, false, 512),     // text-only, fallback
    GEMINI(false, false, 1024),      // Gemini 2.0 Flash
    OPENROUTER(false, false, 1024),  // Gemini 2.0 Flash via OpenRouter
    COHERE(false, false, 1024),      // Aya Vision 32b
    TOGETHER(true, true, 896),       // Gemma 3n native resolution
    OPENAI(true, true, 1024),        // GPT-4o-mini
    ANTHROPIC(true, true, 1568),     // Claude 3.5 Sonnet - Anthropic max recommended
    HYPERBOLIC(true, true, 768),     // Qwen2.5-VL / DeepSeek

    RANDOM(false, false, 1024);

    private boolean paid;
    private boolean active = true;
    private int maxImageDimension;

    AiProvider(boolean paid, boolean active, int maxImageDimension) {
        this.paid = paid;
        this.active = active;
        this.maxImageDimension = maxImageDimension;
    }

    public String getQueueName() {
        return this.name();
    }

    public static AiProvider fromQueueName(String queueName) {
        if (queueName == null) {
            throw new IllegalArgumentException("Queue name cannot be null");
        }
        try {
            return AiProvider.valueOf(queueName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown AI provider queue: " + queueName);
        }
    }

    public boolean supportsVision() {
        // CEREBRAS używa modeli tekstowych bez vision
        return this != CEREBRAS && this != RANDOM;
    }
}
