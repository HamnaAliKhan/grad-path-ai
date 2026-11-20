package ai.gradpath.service.assistant;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Removes contact details and the student's name from text before it is sent to an external AI provider.
 */
public final class PiiRedactor {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("\\+?\\d[\\d\\s\\-().]{8,}\\d");
    private static final Pattern URL = Pattern.compile("https?://\\S+");

    private PiiRedactor() { }

    /**
     * @param text       user text
     * @param nameTokens parts of the student's name (each part of 3+ letters is replaced)
     */
    public static String redact(String text, Collection<String> nameTokens) {
        if (text == null) {
            return "";
        }
        String out = EMAIL.matcher(text).replaceAll("[email removed]");
        out = URL.matcher(out).replaceAll("[link removed]");
        out = PHONE.matcher(out).replaceAll("[phone removed]");
        for (String token : nameTokens) {
            if (token != null && token.strip().length() >= 3) {
                out = Pattern.compile("(?iu)\\b" + Pattern.quote(token.strip()) + "\\b").matcher(out).replaceAll("[name]");
            }
        }
        return out;
    }
}
