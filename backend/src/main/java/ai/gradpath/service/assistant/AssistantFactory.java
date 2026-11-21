package ai.gradpath.service.assistant;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ai.gradpath.config.GradPathProperties;

/**
 * FACTORY PATTERN: chooses the assistant strategy from configuration. An AI provider is used only when
 * CHATBOT_PROVIDER names one AND its API key is configured; otherwise the built-in assistant is used.
 */
@Component
public class AssistantFactory {

    private final GradPathProperties props;
    private final HttpTransport http;
    private final Map<Integer, Integer> callsPerUser = new ConcurrentHashMap<>();

    @Autowired
    public AssistantFactory(GradPathProperties props) {
        this(props, HttpTransport.jdk());
    }

    public AssistantFactory(GradPathProperties props, HttpTransport http) {
        this.props = props;
        this.http = http;
    }

    public AssistantStrategy create() {
        String provider = props.chat().provider() == null ? "rule" : props.chat().provider().strip().toLowerCase();
        String key = props.chat().keyFor(provider);
        if (!"rule".equals(provider) && key != null && !key.isBlank()) {
            return new LlmAssistant(provider, key.strip(), props.chat().modelFor(provider), http, callsPerUser);
        }
        return new RuleBasedAssistant();
    }
}
