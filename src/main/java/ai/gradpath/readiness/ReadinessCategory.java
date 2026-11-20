package ai.gradpath.readiness;

import java.util.Arrays;

/**
 * Readiness categories (rule 60). {@code fitPoints} is the readiness component used in job-fit scores.
 */
public enum ReadinessCategory {
    NEEDS_IMPROVEMENT("Needs Improvement", 0, 25, "warn",
            "Several factors are still early. Focus on the recommended next steps; small steps add up quickly."),
    DEVELOPING("Developing", 40, 60, "info",
            "You are on the right track. A few targeted improvements could move you into Job Ready."),
    JOB_READY("Job Ready", 70, 100, "success",
            "Your profile meets most of the rule thresholds. Keep it current and start applying.");

    /** Score at which "Developing" starts (rule 60). */
    public static final double DEVELOPING_FROM = 40;
    /** Score at which "Job Ready" starts (rule 60). */
    public static final double JOB_READY_FROM = 70;

    private final String label;
    private final double minScore;
    private final int fitPoints;
    private final String badge;
    private final String meaning;

    ReadinessCategory(String label, double minScore, int fitPoints, String badge, String meaning) {
        this.label = label;
        this.minScore = minScore;
        this.fitPoints = fitPoints;
        this.badge = badge;
        this.meaning = meaning;
    }

    /** Rule 60: below 40 = Needs Improvement; 40 to below 70 = Developing; 70 or higher = Job Ready. */
    public static ReadinessCategory forScore(double score) {
        if (score < DEVELOPING_FROM) {
            return NEEDS_IMPROVEMENT;
        }
        if (score < JOB_READY_FROM) {
            return DEVELOPING;
        }
        return JOB_READY;
    }

    /** Labels in order: Needs Improvement, Developing, Job Ready. */
    public static java.util.List<String> labels() {
        return Arrays.stream(values()).map(ReadinessCategory::label).toList();
    }

    /** Category by stored label (unknown labels count as Needs Improvement). */
    public static ReadinessCategory fromLabel(String label) {
        return Arrays.stream(values()).filter(c -> c.label.equals(label)).findFirst().orElse(NEEDS_IMPROVEMENT);
    }

    public String label() { return label; }
    public double minScore() { return minScore; }
    public int fitPoints() { return fitPoints; }
    public String badge() { return badge; }
    public String meaning() { return meaning; }
}
