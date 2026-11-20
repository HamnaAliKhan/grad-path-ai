package ai.gradpath.web;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ai.gradpath.domain.Job;
import ai.gradpath.domain.JobApplication;
import ai.gradpath.domain.Student;
import ai.gradpath.readiness.ReadinessCategory;
import ai.gradpath.reference.CareerCatalog;
import ai.gradpath.security.AuthenticatedUser;
import ai.gradpath.service.ApplicationService;
import ai.gradpath.service.FitCalculator;
import ai.gradpath.service.JobService;
import ai.gradpath.service.NotFoundException;
import ai.gradpath.service.RecommendationService;
import ai.gradpath.service.Result;
import ai.gradpath.service.SkillGapService;

/** Student job board, job details, applying and the application tracker. */
@Controller
public class JobBoardController {

    private final StudentPages pages;
    private final JobService jobs;
    private final ApplicationService applications;
    private final RecommendationService recommendations;
    private final SkillGapService skillGap;

    public JobBoardController(StudentPages pages, JobService jobs, ApplicationService applications,
                              RecommendationService recommendations, SkillGapService skillGap) {
        this.pages = pages;
        this.jobs = jobs;
        this.applications = applications;
        this.recommendations = recommendations;
        this.skillGap = skillGap;
    }

    @GetMapping("/student/jobs")
    public String board(@AuthenticationPrincipal AuthenticatedUser me, @RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "All") String category, @RequestParam(defaultValue = "All") String level,
                        Model model) {
        Student s = pages.current(me);
        String query = q.length() > 80 ? q.substring(0, 80) : q;
        model.addAttribute("q", query);
        model.addAttribute("category", category);
        model.addAttribute("level", level);
        model.addAttribute("categories", CareerCatalog.careerNames());
        model.addAttribute("levels", CareerCatalog.JOB_LEVELS);
        model.addAttribute("ranked", recommendations.rankJobs(s, jobs.search(query, category, level, true)));
        model.addAttribute("applied", applications.appliedJobIds(s.getId()));
        return "student/jobs";
    }

    @GetMapping("/student/jobs/{id}")
    public String detail(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Integer id, Model model) {
        Student s = pages.current(me);
        Job job = jobs.require(id);
        Set<Integer> applied = applications.appliedJobIds(s.getId());
        if (!job.isOpen() && !applied.contains(id)) {
            throw new NotFoundException("This listing is closed.");
        }
        if (!model.containsAttribute("form")) {
            ApplicationService.ApplicationForm form = new ApplicationService.ApplicationForm();
            form.setPhone(s.getPhone());
            form.setPortfolioUrl(s.getPortfolioUrl());
            model.addAttribute("form", form);
        }
        SkillGapService.JobMatch match = skillGap.matchJob(s, job.skillList());
        ReadinessCategory cat = applications.computeFit(s, job, "").readiness().category();
        model.addAttribute("student", s);
        model.addAttribute("job", job);
        model.addAttribute("match", match);
        model.addAttribute("alreadyApplied", applied.contains(id));
        model.addAttribute("availability", CareerCatalog.AVAILABILITY);
        model.addAttribute("readinessCategory", cat);
        model.addAttribute("fitPreview", FitCalculator.explain(match.percent(), 0, cat));
        return "student/job-detail";
    }

    @PostMapping("/student/jobs/{id}/apply")
    public String apply(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Integer id,
                        @ModelAttribute("form") ApplicationService.ApplicationForm form,
                        @RequestParam(value = "resume", required = false) MultipartFile resume, RedirectAttributes ra)
            throws IOException {
        Student s = pages.current(me);
        byte[] pdf = resume == null || resume.isEmpty() ? new byte[0] : resume.getBytes();
        Result<JobApplication> result = applications.apply(s, id, form, pdf, resume == null ? null : resume.getOriginalFilename());
        if (!result.isOk()) {
            Flash.errors(ra, result.errors());
            ra.addFlashAttribute("form", form);
            return "redirect:/student/jobs/" + id + "#apply";
        }
        JobApplication a = result.value();
        Flash.success(ra, "Application sent for \"" + a.getJob().getTitle() + "\". The Career Office can now see it - "
                + "follow its status here.");
        return "redirect:/student/applications";
    }

    @GetMapping("/student/applications")
    public String tracker(@AuthenticationPrincipal AuthenticatedUser me, Model model) {
        Student s = pages.current(me);
        List<JobApplication> list = applications.forStudent(s.getId());
        model.addAttribute("applications", list);
        return "student/applications";
    }

    /** A student may download only the resume they submitted with their own application. */
    @GetMapping("/student/applications/{id}/resume")
    public ResponseEntity<byte[]> ownResume(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Integer id) {
        JobApplication a = applications.requireForStudent(me.id(), id);
        byte[] pdf = applications.resumeForStudent(me.id(), id);
        return pdfResponse(pdf, a.getResumeFilename(), false);
    }

    static ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename, boolean inline) {
        if (pdf == null || pdf.length == 0) {
            throw new NotFoundException("No resume file is stored for this application.");
        }
        String name = filename == null || filename.isBlank() ? "resume.pdf" : filename;
        ContentDisposition cd = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(name, java.nio.charset.StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(pdf);
    }
}
