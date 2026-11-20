package ai.gradpath.readiness;

import java.util.Locale;

/** Number formatting shared by rule explanations. */
public final class ReadinessFormat {

    private ReadinessFormat() { }

    /** 16.0 gives "16", 3.5 gives "3.5". */
    public static String points(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
    }

    /** Two-decimal value, e.g. 0.84. */
    public static String two(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    /** Round to one decimal place (half up). */
    public static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
