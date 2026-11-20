package ai.gradpath.web;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ai.gradpath.domain.ApplicationStatus;
import ai.gradpath.domain.CareerOfficer;
import ai.gradpath.domain.Job;
import ai.gradpath.domain.JobApplication;
import ai.gradpath.readiness.ReadinessCategory;
import ai.gradpath.security.AuthenticatedUser;
import ai.gradpath.service.ApplicationService;
import ai.gradpath.service.FitCalculator;
import ai.gradpath.service.JobService;
import ai.gradpath.service.MessageService;
import ai.gradpath.service.OfficerService;
import ai.gradpath.service.ReportService;
import ai.gradpath.service.Result;

/** Officer application review: ranked by fit, details, resume access and status updates. */
@Controller
public class ApplicationReviewController {

    private final ApplicationService applications;
    private final JobService jobs;
    private final OfficerService officers;
    private final MessageService messages;

    public ApplicationReviewController(ApplicationService applications, JobService jobs, OfficerService officers,
                                       MessageService messages) {
        this.applications = applications;
        this.jobs = jobs;
        this.officers = officers;
        this.messages = messages;
    }

    @GetMapping("/officer/applications")
    public String list(@RequestParam(required = false) Integer job, Model model) {
        List<Job> all = jobs.all();
        Map<Integer, Long> counts = applications.counts();
        model.addAttribute("jobs", all);
        model.addAttribute("counts", counts);
        Integer selected = job;
        if (selected == null) {
            selected = all.stream().filter(j -> counts.getOrDefault(j.getId(), 0L) > 0).map(Job::getId).findFirst()
                    .orElse(all.isEmpty() ? null : all.get(0).getId());
        }
        model.addAttribute("selectedId", selected);
        if (selected != null) {
            Job j = jobs.require(selected);
            List<JobApplication> apps = applications.forJob(selected);
            model.addAttribute("job", j);
            model.addAttribute("applications", apps);
            model.addAttribute("strongFits", apps.stream().filter(a -> a.getFitScore() >= 75).count());
            model.addAttribute("positive", apps.stream().filter(a -> ApplicationStatus.fromLabel(a.getStatus())
                    .map(ApplicationStatus::isPositive).orElse(false)).count());
        }
        return "officer/applications";
    }

    @GetMapping("/officer/applications/{id}")
    public String detail(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Integer id, Model model) {
        JobApplication a = applications.requireForOfficer(id);
        CareerOfficer officer = officers.require(me.id());
        ReadinessCategory cat = ReadinessCategory.fromLabel(a.getReadinessCategory());
        model.addAttribute("app", a);
        model.addAttribute("statuses", ApplicationStatus.labels());
        model.addAttribute("fitExplanation", FitCalculator.explain(a.getSkillMatch(), a.getResumeMatch(), cat));
        model.addAttribute("canMessage", messages.canMessage(officer, a.getStudent()));
        // Fictional demo applications are seeded without a PDF file.
        model.addAttribute("hasResume", !a.getStudent().isDemo());
        return "officer/application-detail";
    }

    @PostMapping("/officer/applications/{id}/status")
    public String status(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Integer id,
                         @RequestParam(defaultValue = "") String status, RedirectAttributes ra) {
        Result<JobApplication> r = applications.updateStatus(officers.require(me.id()), id, status);
        if (r.isOk()) {
            Flash.success(ra, "Status updated to \"" + status + "\".");
        } else {
            Flash.errors(ra, r.errors());
        }
        return "redirect:/officer/applications/" + id;
    }

    /** Resume PDF for authorised officers; inline for preview or as a download. */
    @GetMapping("/officer/applications/{id}/resume")
    public ResponseEntity<byte[]> resume(@PathVariable Integer id, @RequestParam(defaultValue = "false") boolean download) {
        JobApplication a = applications.requireForOfficer(id);
        return JobBoardController.pdfResponse(applications.resumeForOfficer(id), a.getResumeFilename(), !download);
    }

    @GetMapping("/officer/applications/export")
    public ResponseEntity<byte[]> export(@RequestParam Integer job) {
        Job j = jobs.require(job);
        String csv = ReportService.applicantsCsv(j, applications.forJob(job));
        return csvResponse(csv, "applicants_job_" + job + ".csv");
    }

    static ResponseEntity<byte[]> csvResponse(String csv, String filename) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(("﻿" + csv).getBytes(StandardCharsets.UTF_8));
    }
}
