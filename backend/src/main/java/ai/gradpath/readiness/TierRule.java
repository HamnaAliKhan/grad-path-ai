package ai.gradpath.readiness;

/**
 * One tier of a factor: when the measured value lies in [lower, upper) the rule awards {@code points}.
 * Tiers of a factor are contiguous and non-overlapping, so exactly one tier rule applies to any valid value.
 *
 * @param number    the documented rule number (1-57)
 * @param condition plain-language condition, e.g. "Ratio 0.80 to below 0.85"
 * @param lower     inclusive lower bound
 * @param upper     exclusive upper bound ({@link Double#POSITIVE_INFINITY} for the top tier)
 * @param points    points awarded
 */
public record TierRule(int number, String condition, double lower, double upper, double points) {

    public boolean matches(double value) {
        return value >= lower && value < upper;
    }

    /** Plain-language outcome, e.g. "4 points" or "0.5 points". */
    public String outcome() {
        return ReadinessFormat.points(points) + (points == 1 ? " point" : " points");
    }
}
