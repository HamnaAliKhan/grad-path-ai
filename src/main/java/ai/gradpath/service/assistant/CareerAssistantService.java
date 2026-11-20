package ai.gradpath.service.assistant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.gradpath.domain.Student;
import ai.gradpath.readiness.FactorResult;
import ai.gradpath.readiness.ReadinessResult;
import ai.gradpath.reference.CareerCatalog;
import ai.gradpath.service.ApplicationService;
import ai.gradpath.service.GoalService;
import ai.gradpath.service.ReadinessService;
import ai.gradpath.service.SkillGapService;

/** Builds the anonymous context, redacts the conversation and asks the configured assistant strategy. */
@Service
@PreAuthorize("hasRole('STUDENT')")
public class CareerAssistantService {

    public static final int MAX_QUESTION = 800;

    private final AssistantFactory factory;
    private final ReadinessService readiness;
    private final SkillGapService skillGap;
    private final ApplicationService applications;
    private final GoalService goals;

    public CareerAssistantService(AssistantFactory factory, ReadinessService readiness, SkillGapService skillGap,
                                  ApplicationService applications, GoalService goals) {
        this.factory = factory;
        this.readiness = readiness;
        this.skillGap = skillGap;
        this.applications = applications;
        this.goals = goals;
    }

    public String label() {
        return factory.create().label();
    }

    /** Anonymous context: numbers and career names only. */
    @Transactional(readOnly = true)
    public AssistantContext context(Student s) {
        ReadinessResult r = readiness.evaluate(s);
        String career = CareerCatalog.resolveCareer(s.getTargetCareer(), s.getDepartment());
        SkillGapService.Analysis gap = skillGap.analyze(s, career);
        List<String> weakest = r.factors().stream().filter(f -> f.ratio() < 0.85)
                .sorted((a, b) -> Double.compare(b.gap(), a.gap())).limit(2).map(FactorResult::name).toList();
        return new AssistantContext(career, r.score(), r.category().label(), r.preliminary(), weakest,
                gap.priorities().stream().map(SkillGapService.Row::skill).toList(), gap.match(),
                applications.forStudent(s.getId()).size(), goals.summary(goals.list(s.getId())).progress());
    }

    /** Answer the latest question. Contact details and the student's name are removed from every turn first. */
    public AssistantReply ask(Student s, List<ChatTurn> history) {
        List<String> nameTokens = Arrays.asList(s.getName().split("\\s+"));
        List<ChatTurn> clean = new ArrayList<>();
        for (ChatTurn t : history.subList(Math.max(0, history.size() - 12), history.size())) {
            String content = t.content() == null ? "" : t.content();
            if (content.length() > MAX_QUESTION) {
                content = content.substring(0, MAX_QUESTION);
            }
            clean.add(new ChatTurn("assistant".equals(t.role()) ? "assistant" : "user", PiiRedactor.redact(content, nameTokens)));
        }
        return factory.create().reply(clean, context(s), s.getId());
    }
}
