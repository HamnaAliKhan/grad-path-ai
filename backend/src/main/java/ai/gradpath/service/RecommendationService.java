package ai.gradpath.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.springframework.stereotype.Service;

import ai.gradpath.domain.Job;
import ai.gradpath.domain.Student;
import ai.gradpath.readiness.FactorResult;
import ai.gradpath.readiness.ProfileSnapshot;
import ai.gradpath.readiness.ReadinessCategory;
import ai.gradpath.readiness.ReadinessFormat;
import ai.gradpath.readiness.ReadinessResult;

/**
 * Personalised guidance derived from the rule engine:
 * <ul>
 *   <li>next steps for the weakest factors,</li>
 *   <li>"biggest levers": realistic profile changes re-scored by the same deterministic rules,</li>
 *   <li>job ranking by transparent skill match.</li>
 * </ul>
 */
@Service
public class RecommendationService {

    /** A personalised next step. */
    public record Action(String title, String detail, String factor, String link) { }

    /** A realistic improvement and its effect on the rule-based score. */
    public record Lever(String title, String detail, String factor, double gain, double newScore,
                        ReadinessCategory newCategory, boolean movesUp) {
        public String gainLabel() {
            return ReadinessFormat.points(gain);
        }
    }

    /** Ranked levers plus the combined effect of the top three. */
    public record Plan(ReadinessResult baseline, List<Lever> levers, ReadinessResult combined, List<String> combinedTitles) { }

    /** A job with the student's skill match. */
    public record RankedJob(Job job, SkillGapService.JobMatch match) { }

    private record Scenario(String factor, String title, String detail, Predicate<ProfileSnapshot> applies,
                            UnaryOperator<ProfileSnapshot> change) { }

    private static final List<Scenario> SCENARIOS = List.of(
        new Scenario("projects", "Complete 2 more projects",
            "Build and publish two portfolio projects, each with a clear README and a short demo.",
            p -> p.projects() < 7, p -> p.withProjects(p.projects() + 2)),
        new Scenario("experience", "Complete an internship",
            "Apply for internships or virtual experience programmes - one internship already counts.",
            p -> p.internships() < 5, p -> p.withInternships(p.internships() + 1)),
        new Scenario("skills", "Master 2 more technical skills",
            "Pick the top missing skills from Skill-Gap Analysis and prove each with a mini-project.",
            p -> p.confidentSkills() + 0.5 * p.learningSkills() < 8,
            p -> p.withSkills(p.confidentSkills() + 2, Math.max(0, p.learningSkills() - 2))),
        new Scenario("communication", "Raise communication by 2 points",
            "Do mock interviews and present your projects to friends or classmates.",
            p -> p.communication() <= 8, p -> p.withCommunication(Math.min(10, p.communication() + 2))),
        new Scenario("certifications", "Earn a relevant certification",
            "Choose one aligned with your target career and finish it in 4-6 weeks.",
            p -> p.certifications() < 4, p -> p.withCertifications(p.certifications() + 1)),
        new Scenario("extracurricular", "Take a leadership or society role",
            "Lead a club activity, organise an event or join a competition team.",
            p -> p.extracurricular() <= 7, p -> p.withExtracurricular(Math.min(10, p.extracurricular() + 3))),
        new Scenario("cgpa", "Raise your CGPA a little",
            "Target your weakest courses next term (about 5% of your grading scale).",
            p -> p.cgpa() < p.cgpaScale(),
            p -> p.withCgpa(Math.min(p.cgpaScale(), Math.round((p.cgpa() + 0.05 * p.cgpaScale()) * 100) / 100.0)))
    );

    private final ReadinessService readiness;
    private final SkillGapService skillGap;

    public RecommendationService(ReadinessService readiness, SkillGapService skillGap) {
        this.readiness = readiness;
        this.skillGap = skillGap;
    }

    /** Re-score each realistic change with the same rules and rank them by gain. */
    public Plan levers(Student student, int limit) {
        ProfileSnapshot base = ProfileSnapshot.of(student);
        ReadinessResult baseline = readiness.evaluate(base);
        List<Lever> found = new ArrayList<>();
        List<Scenario> used = new ArrayList<>();
        for (Scenario sc : SCENARIOS) {
            if (!sc.applies().test(base)) {
                continue;
            }
            ReadinessResult changed = readiness.evaluate(sc.change().apply(base));
            double gain = ReadinessFormat.round1(changed.score() - baseline.score());
            if (gain > 0) {
                found.add(new Lever(sc.title(), sc.detail(), sc.factor(), gain, changed.score(), changed.category(),
                        changed.category() != baseline.category()));
                used.add(sc);
            }
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < found.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingDouble((Integer i) -> -found.get(i).gain()));
        List<Lever> top = order.stream().limit(limit).map(found::get).toList();
        ReadinessResult combined = null;
        List<String> titles = new ArrayList<>();
        if (top.size() >= 2) {
            ProfileSnapshot merged = base;
            for (Integer i : order.subList(0, Math.min(3, order.size()))) {
                merged = used.get(i).change().apply(merged);
                titles.add(found.get(i).title());
            }
            combined = readiness.evaluate(merged);
        }
        return new Plan(baseline, top, combined, titles);
    }

    /** Next steps for factors below 85% of their weight, largest missing points first. */
    public List<Action> nextSteps(ReadinessResult result, int limit) {
        List<Action> out = new ArrayList<>();
        if (result.preliminary()) {
            out.add(new Action("Complete your profile",
                    "Your result is preliminary. Add: " + String.join(", ", result.missingInformation()) + ".",
                    "profile", "/student/profile/edit"));
        }
        result.factors().stream()
                .filter(f -> f.ratio() < 0.85)
                .sorted(Comparator.comparingDouble(FactorResult::gap).reversed())
                .limit(limit)
                .forEach(f -> out.add(actionFor(f)));
        return out.stream().limit(limit).toList();
    }

    private static Action actionFor(FactorResult f) {
        String lost = ReadinessFormat.points(f.gap()) + " of " + ReadinessFormat.points(f.maxPoints()) + " points still available. ";
        return switch (f.key()) {
            case "cgpa" -> new Action("Lift your academic standing", lost
                    + "Target weaker courses next term and use office hours or study groups.", f.key(), "/student/goals");
            case "projects" -> new Action("Complete a portfolio project", lost
                    + "Build and publish 1-2 projects with a clear README. See Career Explorer for ideas.", f.key(), "/student/careers");
            case "experience" -> new Action("Add relevant experience", lost
                    + "Apply for internships, virtual experience programmes or volunteer tech work.", f.key(), "/student/jobs");
            case "skills" -> new Action("Strengthen a required technical skill", lost
                    + "Open Skill-Gap Analysis and start with the top missing skill.", f.key(), "/student/skill-gap");
            case "certifications" -> new Action("Earn a recognised certification", lost
                    + "Pick one aligned with your target career and finish it in 4-6 weeks.", f.key(), "/student/goals");
            case "communication" -> new Action("Practise communication and interviews", lost
                    + "Do a mock interview and record a 2-minute project pitch.", f.key(), "/student/assistant");
            default -> new Action("Join a society or lead an activity", lost
                    + "Leadership and teamwork examples help in interviews.", f.key(), "/student/goals");
        };
    }

    /** Sort jobs by the student's skill match, best first. */
    public List<RankedJob> rankJobs(Student student, List<Job> jobs) {
        List<RankedJob> ranked = new ArrayList<>(jobs.stream()
                .map(j -> new RankedJob(j, skillGap.matchJob(student, j.skillList()))).toList());
        ranked.sort(Comparator.comparingInt((RankedJob r) -> -r.match().percent()));
        return ranked;
    }
}
