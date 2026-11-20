package ai.gradpath.readiness;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The full, explainable result of the rule engine.
 *
 * @param score              0-100 readiness score (sum of the seven factor points) - not a probability
 * @param category           rule 60 category
 * @param preliminary        rule 59: true when the profile is less than 50% complete
 * @param profileCompletion  profile completion percentage used by rule 59
 * @param missingInformation profile items still missing (shown when preliminary)
 * @param factors            the seven factor results in weight order
 * @param appliedRules       every rule that fired, in ascending order
 * @param engineVersion      rule-engine version stored with history records
 */
public record ReadinessResult(double score, ReadinessCategory category, boolean preliminary, int profileCompletion,
                              List<String> missingInformation, List<FactorResult> factors, List<Integer> appliedRules,
                              String engineVersion) {

    public ReadinessResult {
        missingInformation = List.copyOf(missingInformation);
        factors = List.copyOf(factors);
        appliedRules = List.copyOf(appliedRules);
    }

    /** Factors earning at least 70% of their weight. */
    public List<FactorResult> strengths() {
        return factors.stream().filter(f -> f.ratio() >= 0.7)
                .sorted(Comparator.comparingDouble(FactorResult::points).reversed()).toList();
    }

    /** Factors earning less than 50% of their weight, largest missing points first. */
    public List<FactorResult> weaknesses() {
        return factors.stream().filter(f -> f.ratio() < 0.5)
                .sorted(Comparator.comparingDouble(FactorResult::gap).reversed()).toList();
    }

    public FactorResult factor(String key) {
        return factors.stream().filter(f -> f.key().equals(key)).findFirst().orElseThrow();
    }

    /** Score as text: "62" or "62.5". */
    public String scoreLabel() {
        return ReadinessFormat.points(score);
    }

    /** Whole-number score for progress bars and gauges. */
    public int scorePercent() {
        return (int) Math.round(score);
    }

    /** The sum written out, e.g. "16 + 12 + 5 + 10 + 1 + 7 + 3 = 54". */
    public String calculation() {
        List<String> parts = new ArrayList<>();
        factors.forEach(f -> parts.add(ReadinessFormat.points(f.points())));
        return String.join(" + ", parts) + " = " + scoreLabel();
    }
}
