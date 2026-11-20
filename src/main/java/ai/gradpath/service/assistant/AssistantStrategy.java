package ai.gradpath.service.assistant;

import java.util.List;

/** STRATEGY PATTERN: interchangeable career-assistant reply providers. */
public interface AssistantStrategy {

    /** Label shown above the chat, e.g. "Built-in assistant". */
    String label();

    /**
     * Answer the last user turn.
     *
     * @param history conversation so far (already redacted of contact details)
     * @param context anonymous profile context
     * @param userId  who is asking (used only for the per-user AI request limit, never sent externally)
     */
    AssistantReply reply(List<ChatTurn> history, AssistantContext context, Integer userId);
}
