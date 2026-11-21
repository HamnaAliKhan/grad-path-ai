package ai.gradpath.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A student's application to a job - the ASSOCIATION CLASS between {@link Student} and {@link Job}.
 *
 * <p>The resume PDF bytes (resume_pdf column) are deliberately not mapped: they are read and written only through
 * {@code ApplicationRepository}, so listing applications never loads the files.
 */
@Entity
@Table(name = "job_applications",
        uniqueConstraints = @UniqueConstraint(name = "uq_application_job_student", columnNames = {"job_id", "student_id"}))
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(length = 30, nullable = false)
    private String phone = "";
    @Column(name = "portfolio_url", length = 200, nullable = false)
    private String portfolioUrl = "";
    @Column(length = 60, nullable = false)
    private String availability = "";
    @Column(name = "cover_note", nullable = false, columnDefinition = "text")
    private String coverNote = "";
    @Column(name = "resume_text", nullable = false, columnDefinition = "text")
    private String resumeText = "";
    @Column(name = "resume_filename", length = 160, nullable = false)
    private String resumeFilename = "";
    @Column(name = "skill_match", nullable = false)
    private int skillMatch;
    @Column(name = "resume_match", nullable = false)
    private int resumeMatch;
    @Column(name = "readiness_category", length = 40, nullable = false)
    private String readinessCategory = "";
    /** Readiness score (0-100) at the time of applying. */
    @Column(name = "readiness_pct", nullable = false)
    private double readinessScore;
    @Column(name = "fit_score", nullable = false)
    private int fitScore;
    @Column(length = 30, nullable = false)
    private String status = ApplicationStatus.SUBMITTED.label();
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = Csv.utcNow();
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public JobApplication() { }

    public JobApplication(Job job, Student student) {
        this.job = job;
        this.student = student;
    }

    /** Copy computed fit numbers onto this application. */
    public void applyFit(int skillMatch, int resumeMatch, String readinessCategory, double readinessScore, int fitScore) {
        this.skillMatch = skillMatch;
        this.resumeMatch = resumeMatch;
        this.readinessCategory = readinessCategory;
        this.readinessScore = readinessScore;
        this.fitScore = fitScore;
    }

    public Integer getId() { return id; }
    public Job getJob() { return job; }
    public Student getStudent() { return student; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }
    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }
    public String getCoverNote() { return coverNote; }
    public void setCoverNote(String coverNote) { this.coverNote = coverNote; }
    public String getResumeText() { return resumeText; }
    public void setResumeText(String resumeText) { this.resumeText = resumeText; }
    public String getResumeFilename() { return resumeFilename; }
    public void setResumeFilename(String resumeFilename) { this.resumeFilename = resumeFilename; }
    public int getSkillMatch() { return skillMatch; }
    public int getResumeMatch() { return resumeMatch; }
    public String getReadinessCategory() { return readinessCategory; }
    public double getReadinessScore() { return readinessScore; }
    public int getFitScore() { return fitScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
