package ai.gradpath.service.assistant;

import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Optional AI-provider assistant.
 *
 * <p>FACADE PATTERN: four provider APIs (Groq, OpenAI, Gemini, Anthropic) hide behind one {@link #reply} method.
 * Only redacted conversation text and the anonymous {@link AssistantContext} are sent - never names, emails or phone
 * numbers. API keys travel only in HTTP headers and are never logged. On any failure, or after the per-user limit,
 * the built-in {@link RuleBasedAssistant} answers and the reply says so clearly.
 */
public class LlmAssistant implements AssistantStrategy {

    public static final int MAX_CALLS_PER_USER = 40;

    static final String SYSTEM_PROMPT = "You are GradPath AI's career assistant for university students. Give short, "
            + "practical, encouraging advice about readiness, skills, projects, resumes, internships, interviews, jobs "
            + "and goals. The readiness score is a transparent rule-based score from 0 to 100, not a probability; never "
            + "promise employment. Mention relevant app pages when useful: Placement Readiness, Skill-Gap Analysis, "
            + "Career Explorer, Job Opportunities, Resume Analyzer, Career Goals and Messages. Only discuss careers, "
            + "studying and this app; politely decline anything else. Keep answers under 150 words.";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final String provider;
    private final String apiKey;
    private final String model;
    private final HttpTransport http;
    private final Map<Integer, Integer> callsPerUser;
    private final RuleBasedAssistant fallback = new RuleBasedAssistant();

    public LlmAssistant(String provider, String apiKey, String model, HttpTransport http, Map<Integer, Integer> callsPerUser) {
        this.provider = provider.toLowerCase();
        this.apiKey = apiKey;
        this.model = model;
        this.http = http;
        this.callsPerUser = callsPerUser;
    }

    @Override
    public String label() {
        return Character.toUpperCase(provider.charAt(0)) + provider.substring(1) + " AI";
    }

    @Override
    public AssistantReply reply(List<ChatTurn> history, AssistantContext context, Integer userId) {
        int used = userId == null ? 0 : callsPerUser.getOrDefault(userId, 0);
        if (used >= MAX_CALLS_PER_USER) {
            return fallbackReply(history, context, userId, "AI reply limit for this session reached");
        }
        String system = SYSTEM_PROMPT + "\nAnonymous student context: " + context.summary();
        List<ChatTurn> turns = prepare(history);
        try {
            String text = switch (provider) {
                case "groq" -> openAiStyle("https://api.groq.com/openai/v1/chat/completions", system, turns);
                case "openai" -> openAiStyle("https://api.openai.com/v1/chat/completions", system, turns);
                case "gemini" -> gemini(system, turns);
                case "anthropic" -> anthropic(system, turns);
                default -> throw new IllegalArgumentException("Unsupported provider");
            };
            if (userId != null) {
                callsPerUser.merge(userId, 1, Integer::sum);
            }
            return new AssistantReply(text, label(), null);
        } catch (Exception e) {
            return fallbackReply(history, context, userId, explain(e));
        }
    }

    private AssistantReply fallbackReply(List<ChatTurn> history, AssistantContext context, Integer userId, String reason) {
        AssistantReply r = fallback.reply(history, context, userId);
        return new AssistantReply(r.text(), RuleBasedAssistant.LABEL,
                label() + " is unavailable (" + reason + ") - this answer comes from the built-in assistant.");
    }

    /** Keep the last 8 turns, merge consecutive same-role turns and start with a user turn. */
    static List<ChatTurn> prepare(List<ChatTurn> history) {
        List<ChatTurn> msgs = new ArrayList<>();
        for (ChatTurn t : history) {
            if (!("user".equals(t.role()) || "assistant".equals(t.role())) || t.content() == null || t.content().isBlank()) {
                continue;
            }
            if (!msgs.isEmpty() && msgs.get(msgs.size() - 1).role().equals(t.role())) {
                ChatTurn last = msgs.remove(msgs.size() - 1);
                msgs.add(new ChatTurn(t.role(), last.content() + "\n" + t.content()));
            } else {
                msgs.add(t);
            }
        }
        List<ChatTurn> tail = new ArrayList<>(msgs.subList(Math.max(0, msgs.size() - 8), msgs.size()));
        while (!tail.isEmpty() && !"user".equals(tail.get(0).role())) {
            tail.remove(0);
        }
        return tail;
    }

    private String openAiStyle(String url, String system, List<ChatTurn> turns) throws Exception {
        List<Map<String, String>> msgs = new ArrayList<>();
        msgs.add(Map.of("role", "system", "content", system));
        turns.forEach(t -> msgs.add(Map.of("role", t.role(), "content", t.content())));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", msgs);
        body.put("temperature", 0.5);
        body.put("max_tokens", 700);
        JsonNode data = post(url, Map.of("Authorization", "Bearer " + apiKey), body);
        return nonBlank(data.path("choices").path(0).path("message").path("content").asText(""));
    }

    private String gemini(String system, List<ChatTurn> turns) throws Exception {
        List<Map<String, Object>> contents = new ArrayList<>();
        turns.forEach(t -> contents.add(Map.of("role", "user".equals(t.role()) ? "user" : "model",
                "parts", List.of(Map.of("text", t.content())))));
        Map<String, Object> body = Map.of("system_instruction", Map.of("parts", List.of(Map.of("text", system))),
                "contents", contents, "generationConfig", Map.of("temperature", 0.5, "maxOutputTokens", 700));
        JsonNode data = post("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent",
                Map.of("x-goog-api-key", apiKey), body);
        StringBuilder text = new StringBuilder();
        data.path("candidates").path(0).path("content").path("parts").forEach(p -> text.append(p.path("text").asText("")));
        return nonBlank(text.toString());
    }

    private String anthropic(String system, List<ChatTurn> turns) throws Exception {
        Map<String, Object> body = Map.of("model", model, "max_tokens", 700, "system", system,
                "messages", turns.stream().map(t -> Map.of("role", t.role(), "content", t.content())).toList());
        JsonNode data = post("https://api.anthropic.com/v1/messages",
                Map.of("x-api-key", apiKey, "anthropic-version", "2023-06-01"), body);
        return nonBlank(data.path("content").path(0).path("text").asText(""));
    }

    private JsonNode post(String url, Map<String, String> headers, Object body) throws Exception {
        HttpTransport.Response res = http.postJson(url, headers, JSON.writeValueAsString(body));
        if (res.status() >= 400) {
            throw new ProviderHttpException(res.status());
        }
        return JSON.readTree(res.body());
    }

    private static String nonBlank(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("empty response");
        }
        return text.strip();
    }

    /** A provider returned an HTTP error status. */
    static final class ProviderHttpException extends Exception {
        final int status;

        ProviderHttpException(int status) {
            super("HTTP " + status);
            this.status = status;
        }
    }

    /** Short, non-sensitive reason. */
    static String explain(Exception e) {
        if (e instanceof ProviderHttpException h) {
            return switch (h.status) {
                case 401, 403 -> "the API key was rejected";
                case 404 -> "the configured model was not found";
                case 429 -> "rate limit or quota reached";
                default -> "service error " + h.status;
            };
        }
        if (e instanceof HttpTimeoutException) {
            return "the request timed out";
        }
        return "connection problem";
    }
}
