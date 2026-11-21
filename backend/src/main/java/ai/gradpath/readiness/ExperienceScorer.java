package ai.gradpath.readiness;

import java.util.List;

/**
 * Internship / experience factor (15 points), rules 17-22, plus decision rule 58:
 * with zero internships but at least 12 months of relevant work experience the student receives at least
 * 5 points instead of the zero-internship result. Rule 58 never stacks with internship points.
 */
public class ExperienceScorer extends FactorScorer {

    public static final int WORK_MONTHS_THRESHOLD = 12;
    public static final double WORK_EXPERIENCE_POINTS = 5;

    public ExperienceScorer() {
        super("experience", "Internships / experience", 15, List.of(
                tier(17, "Zero internships", 0, 1, 0),
                tier(18, "One internship", 1, 2, 5),
                tier(19, "Two internships", 2, 3, 9),
                tier(20, "Three internships", 3, 4, 12),
                tier(21, "Four internships", 4, 5, 14),
                tier(22, "Five or more internships", 5, INF, 15)));
    }

    @Override
    protected double measure(ProfileSnapshot p) {
        return p.internships();
    }

    @Override
    protected String describe(ProfileSnapshot p, double value) {
        return p.internships() + " internship" + (p.internships() == 1 ? "" : "s")
                + ", " + p.workExperienceMonths() + " month(s) work experience";
    }

    @Override
    protected String advice(ProfileSnapshot p, double points) {
        if (points >= 9) {
            return "Good practical experience. Describe your impact with numbers on your resume.";
        }
        if (p.internships() == 0) {
            return "An internship is worth 5 points. Virtual experience programmes and volunteer tech work also help.";
        }
        return "A second internship would raise this factor from 5 to 9 points.";
    }

    /** Rule 58: substitute 12+ months of work experience for a missing internship (no stacking). */
    @Override
    protected FactorResult adjust(ProfileSnapshot p, FactorResult tier) {
        if (p.internships() == 0 && p.workExperienceMonths() >= WORK_MONTHS_THRESHOLD
                && tier.points() < WORK_EXPERIENCE_POINTS) {
            String calc = tier.measured() + " - Rule 17 would give 0 points, but Rule 58 (no internship and at least "
                    + WORK_MONTHS_THRESHOLD + " months of relevant work experience) awards "
                    + ReadinessFormat.points(WORK_EXPERIENCE_POINTS) + " of 15 points.";
            return new FactorResult(tier.key(), tier.name(), tier.maxPoints(), WORK_EXPERIENCE_POINTS, tier.measured(),
                    List.of(17, 58), calc,
                    "Your work experience counts like one internship. An internship in your target field can add more.");
        }
        return tier;
    }
}
