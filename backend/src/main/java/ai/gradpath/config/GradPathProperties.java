package ai.gradpath.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Typed application settings. Values come from environment variables (or the git-ignored .env file);
 * see application.properties for the variable names. Secrets are never logged.
 *
 * @param databaseUrl        Neon PostgreSQL connection string (libpq or JDBC form); required
 * @param officerAccessCode  code required for Career Officer registration; blank disables officer sign-up
 * @param demoLoginEnabled   whether the public demo accounts may sign in
 * @param seedDemoData       whether fictional demo records are inserted at start-up (only missing ones)
 * @param defaultCgpaScale   default grading scale offered to students
 * @param cookieSecure       always mark the sign-in cookie Secure (HTTPS only)
 * @param chat               career-assistant provider settings
 */
@ConfigurationProperties(prefix = "gradpath")
public record GradPathProperties(
        @DefaultValue("") String databaseUrl,
        @DefaultValue("") String officerAccessCode,
        @DefaultValue("false") boolean demoLoginEnabled,
        @DefaultValue("false") boolean seedDemoData,
        @DefaultValue("4.0") double defaultCgpaScale,
        @DefaultValue("false") boolean cookieSecure,
        @DefaultValue Chat chat) {

    public static final String APP_NAME = "GradPath AI";
    public static final String APP_FULL_NAME = "Graduate Career Placement Prediction System";
    public static final String TAGLINE = "Your potential. Your pathway. Your next opportunity.";
    public static final String VERSION = "2.0.0";

    /** Grading scales a student may choose. */
    public static final List<Double> CGPA_SCALES = List.of(4.0, 5.0, 10.0);

    public static final String DEMO_STUDENT_EMAIL = "demo@gradpath.ai";
    public static final String DEMO_STUDENT_PASSWORD = "Demo@1234";
    public static final String DEMO_OFFICER_EMAIL = "officer@gradpath.ai";
    public static final String DEMO_OFFICER_PASSWORD = "Officer@1234";

    /** True when officer registration is possible (an access code has been configured). */
    public boolean officerSignupEnabled() {
        return officerAccessCode != null && !officerAccessCode.isBlank();
    }

    /** The configured default scale, or 4.0 when it is not one of the supported scales. */
    public double safeDefaultScale() {
        return CGPA_SCALES.contains(defaultCgpaScale) ? defaultCgpaScale : 4.0;
    }

    /** Career-assistant settings. Keys are optional; without one the built-in assistant is used. */
    public record Chat(
            @DefaultValue("rule") String provider,
            @DefaultValue("") String groqApiKey,
            @DefaultValue("llama-3.3-70b-versatile") String groqModel,
            @DefaultValue("") String openaiApiKey,
            @DefaultValue("gpt-4o-mini") String openaiModel,
            @DefaultValue("") String geminiApiKey,
            @DefaultValue("gemini-2.0-flash") String geminiModel,
            @DefaultValue("") String anthropicApiKey,
            @DefaultValue("claude-haiku-5-5") String anthropicModel) {

        /** The API key for the configured provider, or blank. */
        public String keyFor(String name) {
            return switch (name == null ? "" : name.toLowerCase()) {
                case "groq" -> groqApiKey;
                case "openai" -> openaiApiKey;
                case "gemini" -> geminiApiKey;
                case "anthropic" -> anthropicApiKey;
                default -> "";
            };
        }

        public String modelFor(String name) {
            return switch (name == null ? "" : name.toLowerCase()) {
                case "groq" -> groqModel;
                case "openai" -> openaiModel;
                case "gemini" -> geminiModel;
                case "anthropic" -> anthropicModel;
                default -> "";
            };
        }
    }
}
