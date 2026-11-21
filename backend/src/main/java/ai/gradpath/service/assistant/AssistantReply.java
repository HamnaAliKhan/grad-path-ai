package ai.gradpath.service.assistant;

/**
 * An assistant answer and where it came from.
 *
 * @param text   the answer (plain text, rendered escaped)
 * @param source "Built-in assistant" or e.g. "Groq AI"
 * @param notice optional notice, e.g. why the built-in assistant was used instead of the AI provider
 */
public record AssistantReply(String text, String source, String notice) {
}
