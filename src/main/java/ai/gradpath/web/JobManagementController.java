package ai.gradpath.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ai.gradpath.domain.CareerOfficer;
import ai.gradpath.domain.Job;
import ai.gradpath.reference.CareerCatalog;
import ai.gradpath.security.AuthenticatedUser;
import ai.gradpath.service.ApplicationService;
import ai.gradpath.service.JobService;
import ai.gradpath.service.OfficerService;
import ai.gradpath.service.Result;

/** Officer job management: create listings; edit, open/close and delete only the officer's own listings. */
@Controller
public class JobManagementController {

    private final JobService jobs;
    private final OfficerService officers;
    private final ApplicationService applications;

    public JobManagementController(JobService jobs, OfficerService officers, ApplicationService applications) {
        this.jobs = jobs;
        this.officers = officers;
        this.applications = applications;
    }

    @GetMapping("/officer/jobs")
    public String list(@AuthenticationPrincipal AuthenticatedUser me, @RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "All") String category, @RequestParam(defaultValue = "All") String level,
                       @RequestParam(defaultValue = "all") String owner, Model model) {
        model.addAttribute("jobs", jobs.search(q, category, level, false).stream()
                .filter(j -> !"mine".equals(owner) || j.isManageableBy(me.id())).toList());
        model.addAttribute("counts", applications.counts());
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("level", level);
        model.addAttribute("owner", owner);
        model.addAttribute("categories", CareerCatalog.careerNames());
        model.addAttribute("levels", CareerCatalog.JOB_LEVELS);
        model.addAttribute("meId", me.id());
        return "officer/jobs";
    }

    @GetMapping("/officer/jobs/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new JobService.JobForm());
        }
        options(model);
        model.addAttribute("editing", null);
        return "officer/job-form";
    }

    @PostMapping("/officer/jobs/new")
    public String create(@AuthenticationPrincipal AuthenticatedUser me, @ModelAttribute("form") JobService.JobForm form,
                         Model model, RedirectAttributes ra) {
        CareerOfficer officer = officers.require(me.id());
        Result<Job> r = jobs.create(form, officer);
        if (!r.isOk()) {
            model.addAttribute("formErrors", r.errors());
            options(model);
            model.addAttribute("editing", null);
            return "officer/job-form";
        }
        Flash.success(ra, "Added \"" + r.value().getTitle() + "\". Students can see and apply to it now.");
        return "redirect:/officer/jobs";
    }

    @GetMapping("/officer/jobs/{id}/edit")
    public String editForm(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Integer id, Model model,
                           RedirectAttributes ra) {
        Job job = jobs.require(id);
        if (!job.isManageableBy(me.id())) {
            Flash.error(ra, job.isDemo() ? "Demo listings are read-only." : "You can only edit listings created by your account.");
            return "redirect:/officer/jobs";
        }
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", JobService.JobForm.from(job));
        }
        options(model);
        model.addAttribute("editing", job);
        return "officer/job-form";
    }

    @PostMapping("/officer/jobs/{id}/edit")
    public String update(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Integer id,
                         @ModelAttribute("form") JobService.JobForm form, Model model, RedirectAttributes ra) {
        Result<Job> r = jobs.update(id, form, officers.require(me.id()));
        if (!r.isOk()) {
            model.addAttribute("formErrors", r.errors());
            options(model);
            model.addAttribute("editing", jobs.require(id));
            return "officer/job-form";
        }
        Flash.success(ra, "Updated \"" + r.value().getTitle() + "\".");
        return "redirect:/officer/jobs";
    }

    @PostMapping("/officer/jobs/{id}/toggle")
    public String toggle(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Integer id, RedirectAttributes ra) {
        Job current = jobs.require(id);
        Job job = jobs.setOpen(id, !current.isOpen(), officers.require(me.id()));
        Flash.success(ra, "\"" + job.getTitle() + "\" is now " + (job.isOpen() ? "open" : "closed") + ".");
        return "redirect:/officer/jobs";
    }

    @PostMapping("/officer/jobs/{id}/delete")
    public String delete(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Integer id, RedirectAttributes ra) {
        String title = jobs.delete(id, officers.require(me.id()));
        Flash.success(ra, "Deleted \"" + title + "\" and its applications.");
        return "redirect:/officer/jobs";
    }

    private static void options(Model model) {
        model.addAttribute("categories", CareerCatalog.careerNames());
        model.addAttribute("levels", CareerCatalog.JOB_LEVELS);
        model.addAttribute("skillCatalog", CareerCatalog.SKILL_CATALOG);
        model.addAttribute("skillCategories", CareerCatalog.SKILL_CATEGORIES);
    }
}
