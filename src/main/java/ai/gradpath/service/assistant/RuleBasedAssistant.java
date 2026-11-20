package ai.gradpath.service.assistant;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Deterministic built-in career assistant: the first matching topic rule produces a short, practical answer
 * personalised with anonymous profile numbers. Needs no API key and gives the same answer to the same question.
 */
public class RuleBasedAssistant implements AssistantStrategy {

    public static final String LABEL = "Built-in assistant";

    private record Topic(List<String> keywords, Function<AssistantContext, String> answer) { }

    private static final List<Topic> TOPICS = List.of(
        new Topic(List.of("readiness", "score", "ready", "improve", "category", "rule"), c ->
            "Your rule-based readiness score is " + Math.round(c.score()) + "/100 (" + c.category() + ")"
            + (c.preliminary() ? ", marked preliminary because your profile is under 50% complete" : "") + ". "
            + (c.weakestFactors().isEmpty() ? "No factor is weak right now - keep your profile current. "
                : "The factors with the most points still available are " + String.join(" and ", c.weakestFactors()) + ". ")
            + "Open Placement Readiness to see each rule, your biggest levers and the what-if simulator."),
        new Topic(List.of("skill", "learn", "gap", "course"), c ->
            "For " + c.targetCareer() + " your skill match is " + c.skillMatch() + "%. "
            + (c.missingSkills().isEmpty() ? "You cover the listed example skills - deepen them with projects. "
                : "Start with " + String.join(", ", c.missingSkills()) + ". ")
            + "Learn one skill at a time and prove it with a small project; Skill-Gap Analysis lists tips for each."),
        new Topic(List.of("resume", "cv"), c ->
            "Use Resume Analyzer: upload a text PDF, choose " + c.targetCareer() + " and review keyword coverage, "
            + "structure checks and tips. Lead bullet points with action verbs and add measurable results "
            + "(for example \"cut load time by 30%\"). Only list skills you can demonstrate."),
        new Topic(List.of("internship", "experience", "work"), c ->
            "One internship adds 5 readiness points and two add 9. If you cannot find one yet, 12+ months of relevant "
            + "work experience also counts (rule 58). Check Job Opportunities filtered to the Internship level, "
            + "and consider virtual experience programmes or volunteer tech work."),
        new Topic(List.of("interview", "prepare"), c ->
            "Prepare a 2-minute story for two of your projects (problem, your role, result), rehearse common "
            + c.targetCareer() + " questions, and prepare two questions to ask the interviewer. "
            + "Mock interviews also raise your communication rating."),
        new Topic(List.of("job", "apply", "application", "vacanc", "opportunit", "hire"), c ->
            "Open Job Opportunities: listings are ranked by your skill match. You have sent " + c.applications()
            + " application(s). Each application needs a PDF resume, a link (portfolio, GitHub or LinkedIn) and a "
            + "cover note. Officers rank applications by fit, but every application is reviewed by a person."),
        new Topic(List.of("project", "portfolio", "github"), c ->
            "Each completed project (up to four) adds 4 readiness points. Career Explorer lists example projects for "
            + c.targetCareer() + ". Publish each with a README, screenshots and tests."),
        new Topic(List.of("certif"), c ->
            "Certifications are worth up to 5 points (1 for the first, 3 for two). Choose one aligned with "
            + c.targetCareer() + " that you can finish in 4-6 weeks."),
        new Topic(List.of("communicat", "present", "speak"), c ->
            "Communication is worth up to 10 points (one per rating step). Practise short project pitches, "
            + "join a presentation club and ask for honest feedback before updating your rating."),
        new Topic(List.of("goal", "plan", "task", "track"), c ->
            "Use Career Goals to set a goal and break it into small tasks; progress updates automatically. "
            + "Your current goal progress is " + c.goalProgress() + "%."),
        new Topic(List.of("message", "contact", "officer", "career office"), c ->
            "Use Messages to write directly to a Career Officer. Unread replies are highlighted in the sidebar."),
        new Topic(List.of("hello", " hi ", " hi!", " hey", "salam"), c ->
            "Hi! I can help with readiness, skills, resumes, internships, interviews, jobs and goals. What would you like to work on?")
    );

    static final String DEFAULT_ANSWER = "I can help with improving readiness, choosing skills to learn, resumes, "
            + "internships, interviews, finding jobs and tracking goals. Try one of the suggested questions.";

    @Override
    public String label() {
        return LABEL;
    }

    @Override
    public AssistantReply reply(List<ChatTurn> history, AssistantContext context, Integer userId) {
        String text = history.isEmpty() ? "" : " " + history.get(history.size() - 1).content().toLowerCase(Locale.ROOT) + " ";
        for (Topic t : TOPICS) {
            if (t.keywords().stream().anyMatch(text::contains)) {
                return new AssistantReply(t.answer().apply(context), LABEL, null);
            }
        }
        return new AssistantReply(DEFAULT_ANSWER, LABEL, null);
    }
}
