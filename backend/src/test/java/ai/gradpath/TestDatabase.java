package ai.gradpath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import ai.gradpath.config.DotenvEnvironmentPostProcessor;

/**
 * The database used by integration tests: a separate Neon branch given by {@code TEST_DATABASE_URL}
 * (environment variable, or the git-ignored .env file). The live database ({@code DATABASE_URL}) is never used.
 * When no test database is configured, the database tests are skipped and every other test still runs.
 */
public final class TestDatabase implements ExecutionCondition {

    public static final String VARIABLE = "TEST_DATABASE_URL";

    private static final String URL = resolve();

    /** The test database URL, or blank when none is configured. */
    public static String url() {
        return URL;
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return URL.isBlank()
                ? ConditionEvaluationResult.disabled(VARIABLE + " is not set - database tests skipped (see README, Tests)")
                : ConditionEvaluationResult.enabled("Using the Neon test branch from " + VARIABLE);
    }

    private static String resolve() {
        String env = System.getenv(VARIABLE);
        if (env != null && !env.isBlank()) {
            return env.strip();
        }
        Path dotenv = Path.of(".env");
        if (Files.isRegularFile(dotenv)) {
            try {
                return DotenvEnvironmentPostProcessor.parse(Files.readAllLines(dotenv)).getOrDefault(VARIABLE, "").strip();
            } catch (IOException e) {
                return "";
            }
        }
        return "";
    }
}
