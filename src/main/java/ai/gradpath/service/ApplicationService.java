package ai.gradpath.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.gradpath.domain.ApplicationStatus;
import ai.gradpath.domain.CareerOfficer;
import ai.gradpath.domain.Csv;
import ai.gradpath.domain.Job;
import ai.gradpath.domain.JobApplication;
import ai.gradpath.domain.Student;
import ai.gradpath.readiness.ReadinessResult;
import ai.gradpath.reference.CareerCatalog;
import ai.gradpath.repository.ApplicationRepository;
import ai.gradpath.repository.JobRepository;
import ai.gradpath.validation.Validators;

/** Validate, score, submit and review job applications. */
@Service
public class ApplicationService {

    /** Application form values (mutable for form binding). The resume PDF is passed separately. */
    public static class ApplicationForm {
        private String phone = "";
        private String availability = "";
        private String portfolioUrl = "";
        private String coverNote = "";
        private boolean consent;

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAvailability() { return availability; }
        public void setAvailability(String availability) { this.availability = availability; }
        public String getPortfolioUrl() { return portfolioUrl; }
        public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }
        public String getCoverNote() { return coverNote; }
        public void setCoverNote(String coverNote) { this.coverNote = coverNote; }
        public boolean isConsent() { return consent; }
        public void setConsent(boolean consent) { this.consent = consent; }
    }

    /** Computed fit numbers for a student and job. */
    public record Fit(int skillMatch, int resumeMatch, ReadinessResult readiness, int fitScore) {
        public FitCalculator.FitLabel label() {
            return FitCalculator.FitLabel.of(fitScore);
        }
    }

    private final ApplicationRepository applications;
    private final JobRepository jobs;
    private final SkillGapService skillGap;
    private final ResumeService resumes;
    private final ReadinessService readiness;
    private final ActivityService activity;

    public ApplicationService(ApplicationRepository applications, JobRepository jobs, SkillGapService skillGap,
                              ResumeService resumes, ReadinessService readiness, ActivityService activity) {
        this.applications = applications;
        this.jobs = jobs;
        this.skillGap = skillGap;
        this.resumes = resumes;
        this.readiness = readiness;
        this.activity = activity;
    }

    /** 55% skill match + 25% resume keyword match + 20% readiness-category points. */
    public Fit computeFit(Student student, Job job, String resumeText) {
        int skill = skillGap.matchJob(student, job.skillList()).percent();
        int resume = resumes.skillCoverage(resumeText, job.skillList());
        ReadinessResult r = readiness.evaluate(student);
        return new Fit(skill, resume, r, FitCalculator.fit(skill, resume, r.category()));
    }

    /** Form validation errors, excluding the PDF (empty = valid). */
    public List<String> validate(ApplicationForm f) {
        List<String> errors = new ArrayList<>();
        add(errors, Validators.checkPhone(f.getPhone(), true));
        if (!CareerCatalog.AVAILABILITY.contains(f.getAvailability())) {
            errors.add("Please choose your availability.");
        }
        String link = f.getPortfolioUrl() == null ? "" : f.getPortfolioUrl().strip();
        add(errors, link.isEmpty() ? "Please provide a portfolio, GitHub or LinkedIn link." : Validators.checkUrl(link));
        add(errors, Validators.checkText(f.getCoverNote(), "Cover note", 50, 1500));
        if (!f.isConsent()) {
            errors.add("Please confirm that your details are accurate and may be shared with the Career Office.");
        }
        return errors;
    }

    /**
     * Validate and save an application. Rejects closed jobs and duplicates; the resume text is extracted from the
     * uploaded PDF on the server so the stored text always matches the stored file.
     */
    @PreAuthorize("hasRole('STUDENT')")
    @Transactional
    public Result<JobApplication> apply(Student student, Integer jobId, ApplicationForm form, byte[] pdf, String filename) {
        Job job = jobs.findById(jobId).orElse(null);
        if (job == null || !job.isOpen()) {
            return Result.fail("This position is closed and no longer accepts applications.");
        }
        if (applications.find(jobId, student.getId()).isPresent()) {
            return Result.fail("You have already applied to this job.");
        }
        List<String> errors = validate(form);
        Result<String> text = resumes.extractText(pdf);
        if (!text.isOk()) {
            errors.add(text.error());
        }
        if (!errors.isEmpty()) {
            return Result.fail(errors);
        }
        JobApplication app = new JobApplication(job, student);
        app.setPhone(Validators.cleanPhone(form.getPhone()));
        app.setPortfolioUrl(form.getPortfolioUrl().strip());
        app.setAvailability(form.getAvailability());
        app.setCoverNote(form.getCoverNote().strip());
        app.setResumeText(text.value());
        app.setResumeFilename(safeFilename(filename));
        Fit fit = computeFit(student, job, text.value());
        app.applyFit(fit.skillMatch(), fit.resumeMatch(), fit.readiness().category().label(), fit.readiness().score(),
                fit.fitScore());
        try {
            applications.saveAndFlush(app);
        } catch (DataIntegrityViolationException duplicate) {
            return Result.fail("You have already applied to this job.");
        }
        applications.storeResumePdf(app.getId(), pdf);
        activity.record(student, "Applied for a job", job.getTitle() + " at " + job.getCompany());
        return Result.ok(app);
    }

    @Transactional(readOnly = true)
    public List<JobApplication> forStudent(Integer studentId) {
        return applications.forStudent(studentId);
    }

    @Transactional(readOnly = true)
    public Set<Integer> appliedJobIds(Integer studentId) {
        return applications.jobIdsForStudent(studentId);
    }

    @PreAuthorize("hasRole('OFFICER')")
    @Transactional(readOnly = true)
    public List<JobApplication> forJob(Integer jobId) {
        return applications.forJob(jobId);
    }

    @PreAuthorize("hasRole('OFFICER')")
    @Transactional(readOnly = true)
    public List<JobApplication> all() {
        return applications.everything();
    }

    @Transactional(readOnly = true)
    public Map<Integer, Long> counts() {
        return applications.countsByJob();
    }

    @Transactional(readOnly = true)
    public long awaitingReview() {
        return applications.countByStatus(ApplicationStatus.SUBMITTED.label());
    }

    /** An application an officer may review. */
    @PreAuthorize("hasRole('OFFICER')")
    @Transactional(readOnly = true)
    public JobApplication requireForOfficer(Integer appId) {
        return applications.findById(appId).orElseThrow(() -> new NotFoundException("Application not found."));
    }

    /** A student's own application (other students' applications are reported as not found). */
    @Transactional(readOnly = true)
    public JobApplication requireForStudent(Integer studentId, Integer appId) {
        return applications.findById(appId).filter(a -> a.getStudent().getId().equals(studentId))
                .orElseThrow(() -> new NotFoundException("Application not found."));
    }

    /** Resume bytes for an authorised officer. */
    @PreAuthorize("hasRole('OFFICER')")
    @Transactional(readOnly = true)
    public byte[] resumeForOfficer(Integer appId) {
        requireForOfficer(appId);
        return applications.resumePdf(appId);
    }

    /** Resume bytes for the student who submitted the application. */
    @PreAuthorize("hasRole('STUDENT')")
    @Transactional(readOnly = true)
    public byte[] resumeForStudent(Integer studentId, Integer appId) {
        requireForStudent(studentId, appId);
        return applications.resumePdf(appId);
    }

    /** Change an application's status (officers only, valid statuses only). */
    @PreAuthorize("hasRole('OFFICER')")
    @Transactional
    public Result<JobApplication> updateStatus(CareerOfficer officer, Integer appId, String status) {
        if (ApplicationStatus.fromLabel(status).isEmpty()) {
            return Result.fail("Unknown application status.");
        }
        JobApplication app = requireForOfficer(appId);
        app.setStatus(status);
        app.setUpdatedAt(Csv.utcNow());
        activity.record(officer, "Application status updated", app.getStudent().getName() + " - " + app.getJob().getTitle()
                + ": " + status);
        return Result.ok(app);
    }

    static String safeFilename(String name) {
        String n = name == null ? "" : name.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_").strip();
        if (n.isEmpty()) {
            n = "resume.pdf";
        }
        if (!n.toLowerCase().endsWith(".pdf")) {
            n = n + ".pdf";
        }
        return n.length() > 150 ? n.substring(n.length() - 150) : n;
    }

    private static void add(List<String> errors, String e) {
        if (e != null) {
            errors.add(e);
        }
    }
}
