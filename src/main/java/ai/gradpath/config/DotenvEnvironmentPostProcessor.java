package ai.gradpath.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Loads KEY=value pairs from a git-ignored {@code .env} file for local development.
 *
 * <p>The file is added with the LOWEST precedence, so real environment variables and deployment secrets
 * always win. Set {@code GRADPATH_ENV_FILE} to use another file, or {@code GRADPATH_DOTENV=false} to disable
 * it (the test suite does this so tests can never reach a live database). Values are never logged.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String SOURCE_NAME = "dotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
        if ("false".equalsIgnoreCase(env.getProperty("GRADPATH_DOTENV", "true"))) {
            return;
        }
        Path file = Path.of(env.getProperty("GRADPATH_ENV_FILE", ".env"));
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            Map<String, Object> values = new LinkedHashMap<>(parse(Files.readAllLines(file, StandardCharsets.UTF_8)));
            env.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, values));
        } catch (IOException e) {
            // A missing or unreadable .env simply means "use environment variables only".
        }
    }

    /** Parse KEY=value lines; blank lines and # comments are ignored, matching quotes are removed. */
    public static Map<String, String> parse(List<String> lines) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring(7).strip();
            }
            int eq = line.indexOf('=');
            String key = line.substring(0, eq).strip();
            String value = line.substring(eq + 1).strip();
            if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                    || value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            } else {
                int hash = value.indexOf(" #");
                if (hash >= 0) {
                    value = value.substring(0, hash).strip();
                }
            }
            if (key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                out.put(key, value);
            }
        }
        return out;
    }
}
