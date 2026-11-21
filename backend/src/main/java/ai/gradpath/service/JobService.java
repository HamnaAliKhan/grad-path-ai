package ai.gradpath.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.gradpath.domain.CareerOfficer;
import ai.gradpath.domain.Csv;
import ai.gradpath.domain.Job;
import ai.gradpath.reference.CareerCatalog;
import ai.gradpath.repository.ApplicationRepository;
import ai.gradpath.repository.JobRepository;
import ai.gradpath.validation.Validators;

/**
 * Job listings. Officers may create listings and may edit, open/close or delete ONLY their own listings;
 * seeded demo listings are read-only for everyone.
 */
@Service
public class JobService {

    /** Listing form values (mutable for form binding). */
    public static class JobForm {
        private String title = "";
        private String company = "";
        private String category = "";
        private String level = "";
        private String location = "Remote";
        private List<String> skills = new ArrayList<>();
        private String description = "";

        public static JobForm from(Job j) {
            JobForm f = new JobForm();
            f.title = j.getTitle();
            f.company = j.getCompany();
            f.category = j.getCategory();
            f.level = j.getLevel();
            f.location = j.getLocation();
            f.skills = new ArrayList<>(j.skillList());
            f.description = j.getDescription();
            return f;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCompany() { return company; }
        public void setCompany(String company) { this.company = company; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public List<String> getSkills() { return skills; }
        public void setSkills(List<String> skills) { this.skills = skills == null ? new ArrayList<>() : skills; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    private final JobRepository jobs;
    private final ApplicationRepository applications;
    private final ActivityService activity;

    public JobService(JobRepository jobs, ApplicationRepository applications, ActivityService activity) {
        this.jobs = jobs;
        this.applications = applications;
        this.activity = activity;
    }

    @Transactional(readOnly = true)
    public List<Job> search(String query, String category, String level, boolean onlyOpen) {
        return jobs.search(query, category, level, onlyOpen);
    }

    @Transactional(readOnly = true)
    public List<Job> all() {
        return jobs.search("", "All", "All", false);
    }

    @Transactional(readOnly = true)
    public Job require(Integer id) {
        return jobs.findById(id).orElseThrow(() -> new NotFoundException("Job listing not found."));
    }

    @Transactional(readOnly = true)
    public Optional<Job> find(Integer id) {
        return jobs.findById(id);
    }

    /** Validation errors for a listing (empty = valid). */
    public List<String> validate(JobForm f) {
        List<String> errors = new ArrayList<>();
        add(errors, Validators.checkText(f.getTitle(), "Job title", 3, 140));
        add(errors, Validators.checkText(f.getCompany(), "Company name", 2, 140));
        add(errors, Validators.checkText(f.getLocation(), "Location", 2, 120));
        if (!CareerCatalog.CAREERS.containsKey(f.getCategory())) {
            errors.add("Please choose a career category.");
        }
        if (!CareerCatalog.JOB_LEVELS.contains(f.getLevel())) {
            errors.add("Please choose an experience level.");
        }
        if (f.getSkills().isEmpty()) {
            errors.add("Select at least one required skill.");
        } else if (!CareerCatalog.SKILL_CATALOG.containsAll(f.getSkills())) {
            errors.add("Please choose required skills from the list.");
        } else if (f.getSkills().size() > 12) {
            errors.add("Select at most 12 required skills.");
        }
        add(errors, Validators.checkText(f.getDescription(), "Description", 20, 2000));
        return errors;
    }

    /** Create an open listing owned by the officer. */
    @PreAuthorize("hasRole('OFFICER')")
    @Transactional
    public Result<Job> create(JobForm f, CareerOfficer officer) {
        List<String> errors = validate(f);
        if (!errors.isEmpty()) {
            return Result.fail(errors);
        }
        Job job = new Job();
        apply(job, f);
        job.setCreatedBy(officer.getId());
        job.setOpen(true);
        jobs.save(job);
        activity.record(officer, "Job added", job.getTitle());
        return Result.ok(job);
    }

    @PreAuthorize("hasRole('OFFICER')")
    @Transactional
    public Result<Job> update(Integer jobId, JobForm f, CareerOfficer officer) {
        Job job = manageable(jobId, officer);
        List<String> errors = validate(f);
        if (!errors.isEmpty()) {
            return Result.fail(errors);
        }
        apply(job, f);
        job.setUpdatedAt(Csv.utcNow());
        activity.record(officer, "Job edited", job.getTitle());
        return Result.ok(job);
    }

    @PreAuthorize("hasRole('OFFICER')")
    @Transactional
    public Job setOpen(Integer jobId, boolean open, CareerOfficer officer) {
        Job job = manageable(jobId, officer);
        job.setOpen(open);
        job.setUpdatedAt(Csv.utcNow());
        activity.record(officer, open ? "Job reopened" : "Job closed", job.getTitle());
        return job;
    }

    /** Delete an officer's own listing; its applications are removed by the database cascade. */
    @PreAuthorize("hasRole('OFFICER')")
    @Transactional
    public String delete(Integer jobId, CareerOfficer officer) {
        Job job = manageable(jobId, officer);
        String title = job.getTitle();
        jobs.delete(job);
        activity.record(officer, "Job deleted", title);
        return title;
    }

    /** The listing when the officer owns it and it is not a demo listing; otherwise access is denied. */
    private Job manageable(Integer jobId, CareerOfficer officer) {
        Job job = require(jobId);
        if (!job.isManageableBy(officer.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    job.isDemo() ? "Demo listings are read-only." : "You can only change listings created by your account.");
        }
        return job;
    }

    private static void apply(Job job, JobForm f) {
        job.setTitle(Validators.clean(f.getTitle()));
        job.setCompany(Validators.clean(f.getCompany()));
        job.setCategory(f.getCategory());
        job.setLevel(f.getLevel());
        job.setLocation(Validators.clean(f.getLocation()));
        job.setSkills(Csv.join(f.getSkills()));
        job.setDescription(f.getDescription().strip());
    }

    private static void add(List<String> errors, String e) {
        if (e != null) {
            errors.add(e);
        }
    }
}
