package ai.gradpath.web;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ai.gradpath.domain.CareerGoal;
import ai.gradpath.domain.Student;
import ai.gradpath.readiness.ReadinessResult;
import ai.gradpath.reference.CareerCatalog;
import ai.gradpath.security.AuthenticatedUser;
import ai.gradpath.service.ActivityService;
import ai.gradpath.service.ApplicationService;
import ai.gradpath.service.AuthService;
import ai.gradpath.service.GoalService;
import ai.gradpath.service.JobService;
import ai.gradpath.service.ProfileForm;
import ai.gradpath.service.ReadinessService;
import ai.gradpath.service.RecommendationService;
import ai.gradpath.service.Result;
import ai.gradpath.service.SkillGapService;
import ai.gradpath.service.StudentService;

/** Student onboarding, dashboard and profile. */
@Controller
public class StudentController {

    private final StudentPages pages;
    private final StudentService students;
    private final ReadinessService readiness;
    private final RecommendationService recommendations;
    private final SkillGapService skillGap;
    private final GoalService goals;
    private final JobService jobs;
    private final ApplicationService applications;
    private final ActivityService activity;
    private final AuthService auth;

    public StudentController(StudentPages pages, StudentService students, ReadinessService readiness,
                             RecommendationService recommendations, SkillGapService skillGap, GoalService goals,
                             JobService jobs, ApplicationService applications, ActivityService activity, AuthService auth) {
        this.pages = pages;
        this.students = students;
        this.readiness = readiness;
        this.recommendations = recommendations;
        this.skillGap = skillGap;
        this.goals = goals;
        this.jobs = jobs;
        this.applications = applications;
        this.activity = activity;
        this.auth = auth;
    }

    // ---------- onboarding ----------

    @GetMapping("/onboarding")
    public String onboarding(@AuthenticationPrincipal AuthenticatedUser me, Model model) {
        Student s = pages.current(me);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", pages.initialForm(s));
        }
        pages.formOptions(model);
        model.addAttribute("student", s);
        return "student/onboarding";
    }

    @PostMapping("/onboarding")
    public String finishOnboarding(@AuthenticationPrincipal AuthenticatedUser me, @ModelAttribute("form") ProfileForm form,
                                   Model model, RedirectAttributes ra) {
        Result<Student> result = students.updateProfile(me.id(), form, true);
        if (!result.isOk()) {
            model.addAttribute("formErrors", result.errors());
            pages.formOptions(model);
            model.addAttribute("student", pages.current(me));
            return "student/onboarding";
        }
        Flash.success(ra, "Your profile is set up. This is your dashboard: your readiness score, skills, jobs and next steps are all here.");
        return "redirect:/student";
    }

    // ---------- overview ----------

    @GetMapping("/student")
    public String overview(@AuthenticationPrincipal AuthenticatedUser me, Model model) {
        Student s = pages.current(me);
        ReadinessResult r = readiness.evaluate(s);
        String career = CareerCatalog.resolveCareer(s.getTargetCareer(), s.getDepartment());
        List<CareerGoal> goalList = goals.list(s.getId());
        Set<String> confident = Set.copyOf(s.skillList());
        List<Map.Entry<String, int[]>> categories = CareerCatalog.SKILL_CATEGORIES.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), new int[] {(int) e.getValue().stream().filter(confident::contains).count(),
                        e.getValue().size()}))
                .filter(e -> e.getValue()[0] > 0)
                .sorted((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0])).limit(3).toList();
        model.addAttribute("student", s);
        model.addAttribute("completion", s.profileCompletion());
        model.addAttribute("result", r);
        model.addAttribute("gap", skillGap.analyze(s, career));
        model.addAttribute("skillCategories", categories);
        model.addAttribute("goalSummary", goals.summary(goalList));
        model.addAttribute("actions", recommendations.nextSteps(r, 3));
        model.addAttribute("topJobs", recommendations.rankJobs(s, jobs.search("", "All", "All", true)).stream().limit(3).toList());
        model.addAttribute("applications", applications.forStudent(s.getId()).stream().limit(4).toList());
        model.addAttribute("activity", activity.recent(s.getId(), 6));
        return "student/overview";
    }

    // ---------- profile ----------

    @GetMapping("/student/profile")
    public String profile(@AuthenticationPrincipal AuthenticatedUser me, Model model) {
        Student s = pages.current(me);
        model.addAttribute("student", s);
        model.addAttribute("completion", s.profileCompletion());
        model.addAttribute("sessions", auth.activeSessions(s.getId()));
        return "student/profile";
    }

    @GetMapping("/student/profile/edit")
    public String editProfile(@AuthenticationPrincipal AuthenticatedUser me, Model model) {
        Student s = pages.current(me);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", ProfileForm.from(s));
        }
        pages.formOptions(model);
        model.addAttribute("student", s);
        return "student/profile-edit";
    }

    @PostMapping("/student/profile/edit")
    public String saveProfile(@AuthenticationPrincipal AuthenticatedUser me, @ModelAttribute("form") ProfileForm form,
                              Model model, RedirectAttributes ra) {
        Result<Student> result = students.updateProfile(me.id(), form, false);
        if (!result.isOk()) {
            model.addAttribute("formErrors", result.errors());
            pages.formOptions(model);
            model.addAttribute("student", pages.current(me));
            return "student/profile-edit";
        }
        ReadinessResult r = readiness.evaluate(result.value());
        Flash.success(ra, "Profile saved. Your readiness score is now " + r.scoreLabel() + " (" + r.category().label() + ").");
        return "redirect:/student/profile";
    }
}
