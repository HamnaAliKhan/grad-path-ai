package ai.gradpath.readiness;

import java.util.List;

/** Certifications factor (5 points), rules 32-36. */
public class CertificationScorer extends FactorScorer {

    public CertificationScorer() {
        super("certifications", "Certifications", 5, List.of(
                tier(32, "Zero certifications", 0, 1, 0),
                tier(33, "One certification", 1, 2, 1),
                tier(34, "Two certifications", 2, 3, 3),
                tier(35, "Three certifications", 3, 4, 4),
                tier(36, "Four or more certifications", 4, INF, 5)));
    }

    @Override
    protected double measure(ProfileSnapshot p) {
        return p.certifications();
    }

    @Override
    protected String describe(ProfileSnapshot p, double value) {
        return p.certifications() + " certification" + (p.certifications() == 1 ? "" : "s");
    }

    @Override
    protected String advice(ProfileSnapshot p, double points) {
        if (points >= 4) {
            return "Well certified. Focus effort on projects and experience instead.";
        }
        return "A certification aligned with your target career is a quick, low-risk improvement.";
    }
}
