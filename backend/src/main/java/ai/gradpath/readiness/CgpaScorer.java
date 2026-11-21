package ai.gradpath.readiness;

import java.util.List;

/**
 * CGPA factor (30 points), rules 1-9. CGPA is normalised against the student's grading scale, producing a ratio
 * from 0 to 1 that is rounded to 4 decimals (so 3.2 / 4.0 is exactly 0.80) and clamped to [0, 1].
 */
public class CgpaScorer extends FactorScorer {

    public CgpaScorer() {
        super("cgpa", "CGPA", 30, List.of(
                tier(1, "Ratio below 0.50", 0, 0.50, 0),
                tier(2, "Ratio 0.50 to below 0.60", 0.50, 0.60, 4),
                tier(3, "Ratio 0.60 to below 0.70", 0.60, 0.70, 8),
                tier(4, "Ratio 0.70 to below 0.80", 0.70, 0.80, 12),
                tier(5, "Ratio 0.80 to below 0.85", 0.80, 0.85, 16),
                tier(6, "Ratio 0.85 to below 0.90", 0.85, 0.90, 20),
                tier(7, "Ratio 0.90 to below 0.95", 0.90, 0.95, 24),
                tier(8, "Ratio 0.95 to below 1.00", 0.95, 1.00, 27),
                tier(9, "Ratio exactly 1.00", 1.00, INF, 30)));
    }

    /** CGPA divided by the scale, rounded to 4 decimals and clamped to [0, 1]. */
    public static double ratio(double cgpa, double scale) {
        double r = Math.round(cgpa / scale * 10_000.0) / 10_000.0;
        return Math.max(0, Math.min(1, r));
    }

    @Override
    protected double measure(ProfileSnapshot p) {
        return ratio(p.cgpa(), p.cgpaScale());
    }

    @Override
    protected String describe(ProfileSnapshot p, double value) {
        return "CGPA " + ReadinessFormat.two(p.cgpa()) + " / " + ReadinessFormat.points(p.cgpaScale())
                + " = ratio " + ReadinessFormat.two(value);
    }

    @Override
    protected String advice(ProfileSnapshot p, double points) {
        if (points >= 24) {
            return "Strong academic record - one of your most valuable assets.";
        }
        if (points >= 16) {
            return "A solid academic record. Raising weaker courses can still add points.";
        }
        return "Academic results are the largest factor (30 points). Focus on weaker courses and use office hours.";
    }
}
