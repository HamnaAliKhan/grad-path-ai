package ai.gradpath.readiness;

import java.util.List;

/**
 * The outcome of one factor: the rule(s) that applied, the points earned and a plain-language explanation.
 *
 * @param key          stable identifier, e.g. "cgpa"
 * @param name         display name, e.g. "CGPA"
 * @param maxPoints    the factor's weight (all seven weights add up to 100)
 * @param points       points earned
 * @param measured     the measured value in words, e.g. "3.35 / 4.0 (ratio 0.84)"
 * @param appliedRules rule numbers that determined the points
 * @param calculation  how the points were calculated
 * @param explanation  what this means for the student
 */
public record FactorResult(String key, String name, double maxPoints, double points, String measured,
                           List<Integer> appliedRules, String calculation, String explanation) {

    public FactorResult {
        appliedRules = List.copyOf(appliedRules);
    }

    /** Share of the maximum earned, 0-1. */
    public double ratio() {
        return maxPoints == 0 ? 0 : points / maxPoints;
    }

    /** Share of the maximum earned, 0-100 (whole number). */
    public int percent() {
        return (int) Math.round(100 * ratio());
    }

    /** Points still available on this factor. */
    public double gap() {
        return maxPoints - points;
    }

    public String rulesLabel() {
        return "Rule " + String.join(" + ", appliedRules.stream().map(String::valueOf).toList());
    }
}
