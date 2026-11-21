package ai.gradpath.readiness;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.springframework.stereotype.Component;

/**
 * The deterministic, explainable readiness rule engine (60 documented rules).
 *
 * <ul>
 *   <li>Rules 1-57: tier rules of the seven factors (CGPA 30, Projects 20, Internships/experience 15,
 *       Technical skills 15, Certifications 5, Communication 10, Extracurricular 5 = 100 points).
 *       Exactly one tier rule per factor contributes points.</li>
 *   <li>Rule 58: 12+ months of work experience substitutes for a missing internship (at least 5 points, no stacking).</li>
 *   <li>Rule 59: profile completion below 50% marks the result "Preliminary" and lists the missing information.</li>
 *   <li>Rule 60: category - below 40 Needs Improvement, 40 to below 70 Developing, 70+ Job Ready.</li>
 * </ul>
 *
 * <p>The engine never calls the placement model ({@code ai.gradpath.ml}); the score is a readiness score from 0 to 100,
 * not a calibrated probability of employment, and it is informational only.
 * SINGLETON (Spring bean) and COMPOSITE-style aggregation of {@link FactorScorer} objects.
 */
@Component
public class ReadinessRuleEngine {

    public static final String ENGINE_NAME = "GradPath rule engine";
    public static final String VERSION = "rules-1.0";
    public static final int RULE_COUNT = 60;
    public static final int PRELIMINARY_BELOW = 50;
    public static final String DISCLAIMER = "This readiness score is informational and rule-based. It is an estimate "
            + "for guidance only - not a guarantee of placement, a hiring decision, or a measure of your worth.";

    private final List<FactorScorer> scorers = List.of(new CgpaScorer(), new ProjectScorer(), new ExperienceScorer(),
            new SkillScorer(), new CertificationScorer(), new CommunicationScorer(), new ExtracurricularScorer());

    /** Validate the inputs, apply the 60 rules and explain the result. */
    public ReadinessResult evaluate(ProfileSnapshot p) {
        validate(p);
        List<FactorResult> factors = scorers.stream().map(s -> s.score(p)).toList();
        double total = ReadinessFormat.round1(factors.stream().mapToDouble(FactorResult::points).sum());
        double score = Math.max(0, Math.min(100, total));
        TreeSet<Integer> applied = new TreeSet<>();
        factors.forEach(f -> applied.addAll(f.appliedRules()));
        boolean preliminary = p.profileCompletion() < PRELIMINARY_BELOW;     // rule 59
        if (preliminary) {
            applied.add(59);
        }
        applied.add(60);                                                      // rule 60 always categorises
        return new ReadinessResult(score, ReadinessCategory.forScore(score), preliminary, p.profileCompletion(),
                preliminary ? p.missingInformation() : List.of(), factors, new ArrayList<>(applied), VERSION);
    }

    /**
     * Range checks. Invalid input produces a clear error instead of a silent fallback. Values above a factor's
     * top tier are valid (the top tier applies); a CGPA ratio is clamped to [0, 1] after validation.
     */
    public void validate(ProfileSnapshot p) {
        if (Double.isNaN(p.cgpaScale()) || p.cgpaScale() <= 0 || p.cgpaScale() > 100) {
            throw new InvalidProfileException("cgpaScale", "The CGPA scale must be greater than 0 (for example 4.0, 5.0 or 10.0).");
        }
        if (Double.isNaN(p.cgpa()) || p.cgpa() < 0 || p.cgpa() > p.cgpaScale()) {
            throw new InvalidProfileException("cgpa", "CGPA must be between 0 and " + ReadinessFormat.points(p.cgpaScale()) + ".");
        }
        nonNegative("projects", "Completed projects", p.projects());
        nonNegative("internships", "Internships", p.internships());
        nonNegative("workExperienceMonths", "Work experience months", p.workExperienceMonths());
        nonNegative("confidentSkills", "Confident skills", p.confidentSkills());
        nonNegative("learningSkills", "Skills being learned", p.learningSkills());
        nonNegative("certifications", "Certifications", p.certifications());
        rating("communication", "Communication rating", p.communication());
        rating("extracurricular", "Extracurricular rating", p.extracurricular());
        if (p.profileCompletion() < 0 || p.profileCompletion() > 100) {
            throw new InvalidProfileException("profileCompletion", "Profile completion must be between 0 and 100%.");
        }
    }

    private static void nonNegative(String field, String label, int v) {
        if (v < 0) {
            throw new InvalidProfileException(field, label + " cannot be negative.");
        }
    }

    private static void rating(String field, String label, int v) {
        if (v < 0 || v > 10) {
            throw new InvalidProfileException(field, label + " must be between 0 and 10.");
        }
    }

    public List<FactorScorer> scorers() {
        return scorers;
    }

    /** All 60 rules, numbered 1-60, for documentation and tests. */
    public List<RuleDefinition> rules() {
        List<RuleDefinition> out = new ArrayList<>();
        scorers.forEach(s -> out.addAll(s.definitions()));
        out.add(new RuleDefinition(58, "Decision rule", "Zero internships but at least 12 months of relevant work experience",
                "At least 5 experience points instead of the zero-internship result (never added to internship points)"));
        out.add(new RuleDefinition(59, "Decision rule", "Profile completion below 50%",
                "Result is marked \"Preliminary\" and the missing required information is listed"));
        out.add(new RuleDefinition(60, "Decision rule", "Final score",
                "Below 40 = Needs Improvement; 40 to below 70 = Developing; 70 or higher = Job Ready"));
        out.sort((a, b) -> Integer.compare(a.number(), b.number()));
        return out;
    }
}
