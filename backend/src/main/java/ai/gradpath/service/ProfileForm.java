package ai.gradpath.service;

import java.util.ArrayList;
import java.util.List;

import ai.gradpath.domain.Student;

/**
 * Values of the student profile form (onboarding and "My profile"). A mutable bean so Thymeleaf can bind it.
 * All values are validated by {@link StudentService#validate} before anything is stored.
 */
public class ProfileForm {

    private String name = "";
    private String phone = "";
    private String city = "";
    private String portfolioUrl = "";
    private String university = "";
    private String degree = "";
    private String department = "";
    private Integer graduationYear = 2027;
    private Double cgpa;
    private Double cgpaScale = 4.0;
    private String targetCareer = "";
    private List<String> skills = new ArrayList<>();
    private List<String> learning = new ArrayList<>();
    private String certifications = "";
    private Integer projects = 0;
    private Integer internships = 0;
    private Integer workExperienceMonths = 0;
    private Integer communication = 5;
    private Integer extracurricular = 0;

    /** A form pre-filled from a saved profile. */
    public static ProfileForm from(Student s) {
        ProfileForm f = new ProfileForm();
        f.name = s.getName();
        f.phone = s.getPhone();
        f.city = s.getCity();
        f.portfolioUrl = s.getPortfolioUrl();
        f.university = s.getUniversity();
        f.degree = s.getDegree();
        f.department = s.getDepartment();
        f.graduationYear = s.getGraduationYear();
        f.cgpa = s.getCgpa() > 0 ? s.getCgpa() : null;
        f.cgpaScale = s.getCgpaScale();
        f.targetCareer = s.getTargetCareer();
        f.skills = new ArrayList<>(s.skillList());
        f.learning = new ArrayList<>(s.learningList());
        f.certifications = String.join(", ", s.certList());
        f.projects = s.getProjects();
        f.internships = s.getInternships();
        f.workExperienceMonths = s.getWorkExperienceMonths();
        f.communication = s.getCommunication();
        f.extracurricular = s.getExtracurricular();
        return f;
    }

    /** Certifications split from the comma-separated input. */
    public List<String> certificationList() {
        if (certifications == null || certifications.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(certifications.split("[,\\n]")).map(String::strip)
                .filter(c -> !c.isEmpty()).distinct().toList();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }
    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }
    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Integer getGraduationYear() { return graduationYear; }
    public void setGraduationYear(Integer graduationYear) { this.graduationYear = graduationYear; }
    public Double getCgpa() { return cgpa; }
    public void setCgpa(Double cgpa) { this.cgpa = cgpa; }
    public Double getCgpaScale() { return cgpaScale; }
    public void setCgpaScale(Double cgpaScale) { this.cgpaScale = cgpaScale; }
    public String getTargetCareer() { return targetCareer; }
    public void setTargetCareer(String targetCareer) { this.targetCareer = targetCareer; }
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills == null ? new ArrayList<>() : skills; }
    public List<String> getLearning() { return learning; }
    public void setLearning(List<String> learning) { this.learning = learning == null ? new ArrayList<>() : learning; }
    public String getCertifications() { return certifications; }
    public void setCertifications(String certifications) { this.certifications = certifications; }
    public Integer getProjects() { return projects; }
    public void setProjects(Integer projects) { this.projects = projects; }
    public Integer getInternships() { return internships; }
    public void setInternships(Integer internships) { this.internships = internships; }
    public Integer getWorkExperienceMonths() { return workExperienceMonths; }
    public void setWorkExperienceMonths(Integer v) { this.workExperienceMonths = v; }
    public Integer getCommunication() { return communication; }
    public void setCommunication(Integer communication) { this.communication = communication; }
    public Integer getExtracurricular() { return extracurricular; }
    public void setExtracurricular(Integer extracurricular) { this.extracurricular = extracurricular; }
}
