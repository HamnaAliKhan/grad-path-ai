package ai.gradpath.readiness;

import java.util.List;

/**
 * Base class for the seven readiness factors.
 *
 * <p>TEMPLATE METHOD PATTERN: {@link #score(ProfileSnapshot)} fixes the algorithm - measure the input, pick the single
 * tier rule whose range contains it, build the explanation, then give subclasses a hook ({@link #adjust}) for
 * factor-specific decision rules (used by rule 58). Subclasses only supply the measurement and the wording.
 * INHERITANCE / POLYMORPHISM: the engine treats all seven scorers through this type.
 */
public abstract class FactorScorer {

    private final String key;
    private final String name;
    private final double maxPoints;
    private final List<TierRule> tiers;

    protected FactorScorer(String key, String name, double maxPoints, List<TierRule> tiers) {
        this.key = key;
        this.name = name;
        this.maxPoints = maxPoints;
        this.tiers = List.copyOf(tiers);
    }

    /** The value the tiers are compared with (e.g. CGPA ratio, project count). */
    protected abstract double measure(ProfileSnapshot p);

    /** The measured value in words. */
    protected abstract String describe(ProfileSnapshot p, double value);

    /** Advice for the student given the points earned. */
    protected abstract String advice(ProfileSnapshot p, double points);

    /** Hook for additional decision rules; the default keeps the tier result. */
    protected FactorResult adjust(ProfileSnapshot p, FactorResult tierResult) {
        return tierResult;
    }

    /** Template method: exactly one tier rule contributes the factor's points. */
    public final FactorResult score(ProfileSnapshot p) {
        double value = measure(p);
        TierRule rule = tierFor(value);
        String calc = describe(p, value) + " - Rule " + rule.number() + " (" + rule.condition() + ") gives "
                + ReadinessFormat.points(rule.points()) + " of " + ReadinessFormat.points(maxPoints) + " points.";
        FactorResult base = new FactorResult(key, name, maxPoints, rule.points(), describe(p, value),
                List.of(rule.number()), calc, advice(p, rule.points()));
        return adjust(p, base);
    }

    /** The tier rule whose range contains the value. */
    public TierRule tierFor(double value) {
        return tiers.stream().filter(t -> t.matches(value)).findFirst()
                .orElseThrow(() -> new InvalidProfileException(key, name + " value " + value + " is outside the rule ranges."));
    }

    public String key() { return key; }
    public String name() { return name; }
    public double maxPoints() { return maxPoints; }
    public List<TierRule> tiers() { return tiers; }

    /** This factor's tier rules as documentation rows. */
    public List<RuleDefinition> definitions() {
        return tiers.stream().map(t -> new RuleDefinition(t.number(), name, t.condition(), t.outcome())).toList();
    }

    protected static TierRule tier(int number, String condition, double lower, double upper, double points) {
        return new TierRule(number, condition, lower, upper, points);
    }

    protected static final double INF = Double.POSITIVE_INFINITY;
}
