package ai.gradpath.readiness;

import java.util.List;

/** Technical-skills factor (15 points), rules 23-31. Effective count = confident skills + half the skills being learned. */
public class SkillScorer extends FactorScorer {

    public SkillScorer() {
        super("skills", "Technical skills", 15, List.of(
                tier(23, "Effective count below 0.5", 0, 0.5, 0),
                tier(24, "Effective count 0.5 to below 1.5", 0.5, 1.5, 2),
                tier(25, "Effective count 1.5 to below 2.5", 1.5, 2.5, 4),
                tier(26, "Effective count 2.5 to below 3.5", 2.5, 3.5, 6),
                tier(27, "Effective count 3.5 to below 4.5", 3.5, 4.5, 8),
                tier(28, "Effective count 4.5 to below 5.5", 4.5, 5.5, 10),
                tier(29, "Effective count 5.5 to below 6.5", 5.5, 6.5, 12),
                tier(30, "Effective count 6.5 to below 8", 6.5, 8, 14),
                tier(31, "Effective count 8 or more", 8, INF, 15)));
    }

    public static double effectiveCount(int confident, int learning) {
        return confident + 0.5 * learning;
    }

    @Override
    protected double measure(ProfileSnapshot p) {
        return effectiveCount(p.confidentSkills(), p.learningSkills());
    }

    @Override
    protected String describe(ProfileSnapshot p, double value) {
        return p.confidentSkills() + " confident + 0.5 x " + p.learningSkills() + " learning = "
                + ReadinessFormat.points(value) + " effective skills";
    }

    @Override
    protected String advice(ProfileSnapshot p, double points) {
        if (points >= 14) {
            return "A broad skill set. Prove the key ones with projects.";
        }
        return "Finishing a skill you are learning adds half a skill; each full skill moves you toward the next tier.";
    }
}
