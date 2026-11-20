package ai.gradpath.web;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ai.gradpath.domain.CareerOfficer;
import ai.gradpath.domain.Job;
import ai.gradpath.domain.JobApplication;
import ai.gradpath.domain.Student;
import ai.gradpath.readiness.ReadinessResult;
import ai.gradpath.reference.CareerCatalog;
import ai.gradpath.security.AuthenticatedUser;
import ai.gradpath.service.ActivityService;
import ai.gradpath.service.ApplicationService;
import ai.gradpath.service.AuthService;
import ai.gradpath.service.JobService;
import ai.gradpath.service.MessageService;
import ai.gradpath.service.OfficerService;
import ai.gradpath.service.ReadinessService;
import ai.gradpath.service.ReportService;
import ai.gradpath.service.Result;
import ai.gradpath.service.SkillGapService;
import ai.gradpath.service.StudentService;

/** Career Officer overview, student directory/profile review and officer profile. */
@Controller
public class OfficerController {

    private final OfficerService officers;
    private final ReportService reports;
    private final JobService jobs;
    private final ApplicationService applications;
    private final MessageService messages;
    private final ActivityService activity;
    private final StudentService students;
    private final ReadinessService readiness;
    private final SkillGapService skillGap;
    private final AuthService auth;
    private final ai.gradpath.service.PlacementModelService placementModel;

    public OfficerController(OfficerService officers, ReportService reports, JobService jobs,
                             ApplicationService applications, MessageService messages, ActivityService activity,
                             StudentService students, ReadinessService readiness, SkillGapService skillGap, AuthService auth,
                             ai.gradpath.service.PlacementModelService placementModel) {
        this.placementModel = placementModel;
        this.officers = officers;
        this.reports = reports;
        this.jobs = jobs;
        this.applications = applications;
        this.messages = messages;
        this.activity = activity;
        this.students = students;
        this.readiness = readiness;
        this.skillGap = skillGap;
        this.auth = auth;
    }

    @GetMapping("/officer")
    public String overview(@AuthenticationPrincipal AuthenticatedUser me,
                           @RequestParam(defaultValue = "true") boolean demo, Model model) {
        ReportService.Snapshot snap = reports.snapshot(demo);
        List<ReportService.StudentRow> rows = snap.students();
        List<Job> all = jobs.all();
        List<ReportService.DepartmentRow> depts = ReportService.departments(rows);
        Map<String, Long> cats = ReportService.categoryCounts(rows);
        model.addAttribute("includeDemo", demo);
        model.addAttribute("snapshot", snap);
        model.addAttribute("rows", rows);
        model.addAttribute("categoryCounts", cats);
        model.addAttribute("avgCgpa", ReportService.average(rows, ReportService.StudentRow::cgpa4));
        model.addAttribute("avgScore", ReportService.average(rows, ReportService.StudentRow::score));
        model.addAttribute("withInternship", rows.stream().filter(r -> r.internships() > 0).count());
        model.addAttribute("openJobs", all.stream().filter(Job::isOpen).count());
        model.addAttribute("totalJobs", all.size());
        model.addAttribute("totalApplications", applications.counts().values().stream().mapToLong(Long::longValue).sum());
        model.addAttribute("awaiting", applications.awaitingReview());
        model.addAttribute("recentProfiles", activity.recentProfiles(6));
        model.addAttribute("donutChart", ChartFactory.readinessDonut(cats, "Readiness category distribution"));
        model.addAttribute("deptChart", ChartFactory.departmentBar(depts));
        model.addAttribute("donutInsight", ReportService.readinessInsight(rows));
        model.addAttribute("deptInsight", ReportService.departmentInsight(depts));
        return "officer/overview";
    }

    @GetMapping("/officer/students")
    public String directory(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "All") String department,
                            @RequestParam(defaultValue = "true") boolean demo, Model model) {
        List<Student> list = students.directory(demo, q, department);
        model.addAttribute("students", list.stream().map(s -> new DirectoryRow(s, readiness.evaluate(s))).toList());
        model.addAttribute("q", q);
        model.addAttribute("department", department);
        model.addAttribute("departments", CareerCatalog.DEPARTMENTS);
        model.addAttribute("includeDemo", demo);
        return "officer/students";
    }

    /** One directory row: a student and their current rule-based result. */
    public record DirectoryRow(Student student, ReadinessResult result) { }

    @GetMapping("/officer/students/{id}")
    public String studentProfile(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Integer id, Model model) {
        Student s = students.require(id);
        String career = CareerCatalog.resolveCareer(s.getTargetCareer(), s.getDepartment());
        List<JobApplication> apps = applications.forStudent(s.getId());
        CareerOfficer officer = officers.require(me.id());
        model.addAttribute("student", s);
        model.addAttribute("result", readiness.evaluate(s));
        model.addAttribute("estimate", placementModel.estimate(s).orElse(null));
        model.addAttribute("gap", skillGap.analyze(s, career));
        model.addAttribute("applications", apps);
        model.addAttribute("history", readiness.history(s.getId()));
        model.addAttribute("canMessage", messages.canMessage(officer, s));
        // Contact details are shown only when the student has shared them through an application.
        model.addAttribute("sharedContact", !apps.isEmpty());
        return "officer/student-detail";
    }

    @GetMapping("/officer/profile")
    public String profile(@AuthenticationPrincipal AuthenticatedUser me, Model model) {
        model.addAttribute("officer", officers.require(me.id()));
        model.addAttribute("sessions", auth.activeSessions(me.id()));
        return "officer/profile";
    }

    @PostMapping("/officer/profile")
    public String saveProfile(@AuthenticationPrincipal AuthenticatedUser me, @RequestParam(defaultValue = "") String name,
                              @RequestParam(defaultValue = "") String office, RedirectAttributes ra) {
        Result<CareerOfficer> r = officers.updateProfile(me.id(), name, office);
        if (r.isOk()) {
            Flash.success(ra, "Profile saved.");
        } else {
            Flash.errors(ra, r.errors());
        }
        return "redirect:/officer/profile";
    }
}
