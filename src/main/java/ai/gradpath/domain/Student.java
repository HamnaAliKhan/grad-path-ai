package ai.gradpath.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Student account, academic profile and readiness inputs.
 *
 * <p>COMPOSITION: a student owns its career goals (cascade + orphan removal), and each goal owns its tasks.
 * No sensitive demographic data (gender, age, ethnicity, religion, IQ ...) is collected or stored.
 */
@Entity
@Table(name = "students")
@DiscriminatorValue("student")
@PrimaryKeyJoinColumn(name = "id")
public class Student extends User {

    @Column(length = 160, nullable = false)
    private String university = "";
    @Column(length = 40, nullable = false)
    private String degree = "";
    @Column(length = 80, nullable = false)
    private String department = "";
    @Column(name = "graduation_year", nullable = false)
    private int graduationYear = 2027;
    @Column(nullable = false)
    private double cgpa;
    @Column(name = "cgpa_scale", nullable = false)
    private double cgpaScale = 4.0;
    /** Confident skills, comma separated. */
    @Column(nullable = false, columnDefinition = "text")
    private String skills = "";
    /** Skills being learned, comma separated. */
    @Column(name = "learning_skills", nullable = false, columnDefinition = "text")
    private String learningSkills = "";
    @Column(nullable = false, columnDefinition = "text")
    private String certifications = "";
    @Column(nullable = false)
    private int projects;
    @Column(nullable = false)
    private int internships;
    @Column(name = "work_experience_months", nullable = false)
    private int workExperienceMonths;
    /** Self rating 0-10 (0-1 means "not rated yet"). */
    @Column(nullable = false)
    private int communication;
    /** Activity rating 0-10. */
    @Column(nullable = false)
    private int extracurricular;
    @Column(name = "target_career", length = 80, nullable = false)
    private String targetCareer = "";
    @Column(name = "is_demo", nullable = false)
    private boolean demo;
    @Column(length = 30, nullable = false)
    private String phone = "";
    @Column(length = 80, nullable = false)
    private String city = "";
    @Column(name = "portfolio_url", length = 200, nullable = false)
    private String portfolioUrl = "";

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<CareerGoal> goals = new ArrayList<>();

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    @Override
    public boolean isDemo() {
        return demo;
    }

    public List<String> skillList() {
        return Csv.split(skills);
    }

    public List<String> learningList() {
        return Csv.split(learningSkills);
    }

    public List<String> certList() {
        return Csv.split(certifications);
    }

    /** True once the essentials are filled in; until then the student is sent to onboarding. */
    public boolean isOnboarded() {
        return !university.isBlank() && !department.isBlank() && cgpa > 0 && !skillList().isEmpty()
                && !targetCareer.isBlank();
    }

    /** Percentage of profile items completed and the labels of the missing ones. */
    public ProfileCompletion profileCompletion() {
        return ProfileCompletion.of(this);
    }

    public void addGoal(CareerGoal goal) {
        goal.setStudent(this);
        goals.add(goal);
    }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }
    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public int getGraduationYear() { return graduationYear; }
    public void setGraduationYear(int graduationYear) { this.graduationYear = graduationYear; }
    public double getCgpa() { return cgpa; }
    public void setCgpa(double cgpa) { this.cgpa = cgpa; }
    public double getCgpaScale() { return cgpaScale; }
    public void setCgpaScale(double cgpaScale) { this.cgpaScale = cgpaScale; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public String getLearningSkills() { return learningSkills; }
    public void setLearningSkills(String learningSkills) { this.learningSkills = learningSkills; }
    public String getCertifications() { return certifications; }
    public void setCertifications(String certifications) { this.certifications = certifications; }
    public int getProjects() { return projects; }
    public void setProjects(int projects) { this.projects = projects; }
    public int getInternships() { return internships; }
    public void setInternships(int internships) { this.internships = internships; }
    public int getWorkExperienceMonths() { return workExperienceMonths; }
    public void setWorkExperienceMonths(int months) { this.workExperienceMonths = months; }
    public int getCommunication() { return communication; }
    public void setCommunication(int communication) { this.communication = communication; }
    public int getExtracurricular() { return extracurricular; }
    public void setExtracurricular(int extracurricular) { this.extracurricular = extracurricular; }
    public String getTargetCareer() { return targetCareer; }
    public void setTargetCareer(String targetCareer) { this.targetCareer = targetCareer; }
    public void setDemo(boolean demo) { this.demo = demo; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }
    public List<CareerGoal> getGoals() { return goals; }
}
