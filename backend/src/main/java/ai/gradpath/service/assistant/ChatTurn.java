package ai.gradpath.service.assistant;

/** One chat turn; role is "user" or "assistant". */
public record ChatTurn(String role, String content) {
}
