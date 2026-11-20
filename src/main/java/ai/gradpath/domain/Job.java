package ai.gradpath.domain;

import java.time.LocalDateTime;
import java.util.List;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A job listing. Demo listings are fictional and read-only; real listings are owned by the officer who created them. */
@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(length = 140, nullable = false)
    private String title;
    @Column(length = 140, nullable = false)
    private String company;
    @Column(length = 80, nullable = false)
    private String category;
    @Column(length = 40, nullable = false)
    private String level;
    @Column(nullable = false, columnDefinition = "text")
    private String skills = "";
    @Column(nullable = false, columnDefinition = "text")
    private String description = "";
    @Column(length = 120, nullable = false)
    private String location = "";
    @Column(name = "is_demo", nullable = false)
    private boolean demo;
    @Column(name = "created_by")
    private Integer createdBy;
    @Column(name = "is_open", nullable = false)
    private boolean open = true;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = Csv.utcNow();
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Read-only object view of created_by (the aggregation end of CareerOfficer.jobs). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private CareerOfficer creator;

    public Job() { }

    public Job(String title, String company, String category, String level, String skills,
               String description, String location) {
        this.title = title;
        this.company = company;
        this.category = category;
        this.level = level;
        this.skills = skills;
        this.description = description;
        this.location = location;
    }

    public List<String> skillList() {
        return Csv.split(skills);
    }

    /** Whether the given officer may edit, close or delete this listing. */
    public boolean isManageableBy(Integer officerId) {
        return !demo && officerId != null && officerId.equals(createdBy);
    }

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public CareerOfficer getCreator() { return creator; }
}
