package ai.gradpath.service;

import ai.gradpath.readiness.ReadinessCategory;

/**
 * Explainable job-fit score used to RANK applications (a screening aid, never an automatic hiring decision):
 * fit = 55% skill match + 25% resume keyword match + 20% readiness-category points
 * (Job Ready 100, Developing 60, Needs Improvement 25).
 */
public final class FitCalculator {

    public static final double W_SKILL = 0.55;
    public static final double W_RESUME = 0.25;
    public static final double W_READINESS = 0.20;

    private FitCalculator() { }

    /** Fit label and badge style. */
    public enum FitLabel {
        STRONG("Strong fit", "success"), GOOD("Good fit", "info"), PARTIAL("Partial fit", "warn"), LOW("Low fit", "neutral");

        private final String label;
        private final String badge;

        FitLabel(String label, String badge) {
            this.label = label;
            this.badge = badge;
        }

        public String label() { return label; }
        public String badge() { return badge; }

        /** Strong 75+, Good 55-74, Partial 35-54, Low below 35. */
        public static FitLabel of(int fit) {
            if (fit >= 75) {
                return STRONG;
            }
            if (fit >= 55) {
                return GOOD;
            }
            if (fit >= 35) {
                return PARTIAL;
            }
            return LOW;
        }
    }

    /** Weighted fit score 0-100. */
    public static int fit(int skillMatch, int resumeMatch, ReadinessCategory category) {
        double v = W_SKILL * clamp(skillMatch) + W_RESUME * clamp(resumeMatch) + W_READINESS * category.fitPoints();
        return (int) Math.round(v);
    }

    /** The calculation written out for officers and students. */
    public static String explain(int skillMatch, int resumeMatch, ReadinessCategory category) {
        return "0.55 x " + clamp(skillMatch) + " (skills) + 0.25 x " + clamp(resumeMatch) + " (resume) + 0.20 x "
                + category.fitPoints() + " (" + category.label() + ") = " + fit(skillMatch, resumeMatch, category);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }
}
