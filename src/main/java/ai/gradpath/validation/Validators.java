package ai.gradpath.validation;

import java.util.regex.Pattern;

/**
 * Server-side input validation. Each check returns a user-facing error message, or {@code null} when the value is
 * valid. All user input is validated here before it is stored, regardless of any browser-side checks.
 */
public final class Validators {

    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9\\-]+(\\.[A-Za-z0-9\\-]+)*\\.[A-Za-z]{2,}$");
    /** Letters (any language) separated by spaces, dots, apostrophes or hyphens; optional final dot. */
    private static final Pattern NAME = Pattern.compile("^\\p{L}+(?:[ .'\\-]+\\p{L}+)*\\.?$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{10,14}$");
    private static final Pattern URL = Pattern.compile("^https?://[A-Za-z0-9.-]+\\.[A-Za-z]{2,}(:[0-9]{1,5})?(/[^\\s<>\"]*)?$");
    private static final Pattern LETTER = Pattern.compile("\\p{L}");
    private static final Pattern DIGIT = Pattern.compile("\\d");

    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 72;   // BCrypt only uses the first 72 bytes

    private Validators() { }

    /** Remove common visual separators from a phone number. */
    public static String cleanPhone(String value) {
        return value == null ? "" : value.replaceAll("[\\s\\-().]", "");
    }

    /** Trim, collapse inner whitespace. */
    public static String clean(String value) {
        return value == null ? "" : value.strip().replaceAll("\\s+", " ");
    }

    /** Lower-case, trimmed email. */
    public static String normalizeEmail(String value) {
        return value == null ? "" : value.strip().toLowerCase();
    }

    public static String checkName(String value) {
        return checkName(value, "Full name");
    }

    public static String checkName(String value, String label) {
        String v = clean(value);
        if (v.length() < 2) {
            return label + " must be at least 2 characters.";
        }
        if (v.length() > 80 || !NAME.matcher(v).matches()) {
            return label + " can only contain letters, spaces, apostrophes, dots and hyphens (max 80 characters).";
        }
        return null;
    }

    public static String checkEmail(String value) {
        String v = value == null ? "" : value.strip();
        if (v.isEmpty()) {
            return "Email address is required.";
        }
        if (v.length() > 150 || !EMAIL.matcher(v).matches()) {
            return "Enter a valid email address, for example name@university.edu.";
        }
        return null;
    }

    /** Length 8-72, at least one letter and one digit, and (when given) a matching confirmation. */
    public static String checkPassword(String pw, String confirm) {
        String p = pw == null ? "" : pw;
        if (p.length() < PASSWORD_MIN) {
            return "Password must be at least " + PASSWORD_MIN + " characters.";
        }
        if (p.length() > PASSWORD_MAX) {
            return "Password must be at most " + PASSWORD_MAX + " characters.";
        }
        if (!LETTER.matcher(p).find() || !DIGIT.matcher(p).find()) {
            return "Password needs at least one letter and one number.";
        }
        if (confirm != null && !p.equals(confirm)) {
            return "Passwords do not match.";
        }
        return null;
    }

    public static String checkPhone(String value, boolean required) {
        String v = cleanPhone(value);
        if (v.isEmpty()) {
            return required ? "Phone number is required." : null;
        }
        if (!PHONE.matcher(v).matches()) {
            return "Enter a valid phone number with 10-14 digits, for example +923001234567.";
        }
        return null;
    }

    /** Optional http(s) link. */
    public static String checkUrl(String value) {
        String v = value == null ? "" : value.strip();
        if (v.isEmpty()) {
            return null;
        }
        if (v.length() > 200 || !URL.matcher(v).matches()) {
            return "Links must be a valid web address starting with http:// or https:// (for example https://github.com/you).";
        }
        return null;
    }

    public static String checkText(String value, String label, int minLen, int maxLen) {
        String v = value == null ? "" : value.strip();
        if (v.length() < minLen) {
            return minLen <= 1 ? label + " is required." : label + " must be at least " + minLen + " characters.";
        }
        if (v.length() > maxLen) {
            return label + " must be at most " + maxLen + " characters.";
        }
        return null;
    }

    /** Integer in [min, max] (null counts as missing). */
    public static String checkRange(Integer value, String label, int min, int max) {
        if (value == null) {
            return label + " is required.";
        }
        if (value < min || value > max) {
            return label + " must be between " + min + " and " + max + ".";
        }
        return null;
    }
}
