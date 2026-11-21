package ai.gradpath.service;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.gradpath.config.GradPathProperties;
import ai.gradpath.domain.Csv;
import ai.gradpath.domain.Student;
import ai.gradpath.reference.CareerCatalog;
import ai.gradpath.repository.StudentRepository;
import ai.gradpath.validation.Validators;

/** Retrieve, validate and update student profiles. */
@Service
public class StudentService {

    public static final int MAX_PROJECTS = 50;
    public static final int MAX_INTERNSHIPS = 20;
    public static final int MAX_WORK_MONTHS = 600;
    public static final int MAX_CERTIFICATIONS = 15;

    private final StudentRepository students;
    private final ReadinessService readiness;
    private final ActivityService activity;
    private final ReportService reports;

    public StudentService(StudentRepository students, ReadinessService readiness, ActivityService activity,
                          ReportService reports) {
        this.students = students;
        this.readiness = readiness;
        this.activity = activity;
        this.reports = reports;
    }

    @Transactional(readOnly = true)
    public Student require(Integer id) {
        return students.findById(id).orElseThrow(() -> new NotFoundException("Student not found."));
    }

    @Transactional(readOnly = true)
    public Optional<Student> find(Integer id) {
        return students.findById(id);
    }

    /** Student directory for officers. */
    @PreAuthorize("hasRole('OFFICER')")
    @Transactional(readOnly = true)
    public List<Student> directory(boolean includeDemo, String query, String department) {
        String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        return students.listStudents(includeDemo).stream()
                .filter(Student::isOnboarded)
                .filter(s -> q.isEmpty() || s.getName().toLowerCase(Locale.ROOT).contains(q)
                        || s.getTargetCareer().toLowerCase(Locale.ROOT).contains(q))
                .filter(s -> department == null || department.isBlank() || "All".equals(department)
                        || department.equals(s.getDepartment()))
                .toList();
    }

    /** Every validation error in the form (empty = valid). */
    public List<String> validate(ProfileForm f) {
        List<String> errors = new ArrayList<>();
        add(errors, Validators.checkName(f.getName()));
        add(errors, Validators.checkPhone(f.getPhone(), true));
        add(errors, Validators.checkText(f.getCity(), "City", 2, 60));
        add(errors, Validators.checkUrl(f.getPortfolioUrl()));
        add(errors, Validators.checkText(f.getUniversity(), "University", 3, 120));
        if (!CareerCatalog.DEGREES.contains(f.getDegree())) {
            errors.add("Please choose your degree.");
        }
        if (!CareerCatalog.DEPARTMENTS.contains(f.getDepartment())) {
            errors.add("Please choose your department.");
        }
        int thisYear = Year.now().getValue();
        add(errors, Validators.checkRange(f.getGraduationYear(), "Graduation year", thisYear - 10, thisYear + 8));
        Double scale = f.getCgpaScale();
        if (scale == null || !GradPathProperties.CGPA_SCALES.contains(scale)) {
            errors.add("Please choose a CGPA grading scale (4.0, 5.0 or 10.0).");
        } else if (f.getCgpa() == null || f.getCgpa().isNaN() || f.getCgpa() <= 0 || f.getCgpa() > scale) {
            errors.add("CGPA must be greater than 0 and at most " + (scale % 1 == 0 ? String.valueOf(scale.intValue()) : scale) + ".");
        }
        if (!CareerCatalog.CAREERS.containsKey(f.getTargetCareer())) {
            errors.add("Please choose a target career.");
        }
        if (f.getSkills().isEmpty()) {
            errors.add("Select at least one skill you are confident in.");
        }
        if (!CareerCatalog.SKILL_CATALOG.containsAll(f.getSkills()) || !CareerCatalog.SKILL_CATALOG.containsAll(f.getLearning())) {
            errors.add("Please choose skills from the list.");
        }
        List<String> certs = f.certificationList();
        if (certs.size() > MAX_CERTIFICATIONS) {
            errors.add("List at most " + MAX_CERTIFICATIONS + " certifications.");
        }
        if (certs.stream().anyMatch(c -> c.length() > 80 || c.contains("<") || c.contains(">"))) {
            errors.add("Each certification name must be at most 80 characters, without < or >.");
        }
        add(errors, Validators.checkRange(f.getProjects(), "Completed projects", 0, MAX_PROJECTS));
        add(errors, Validators.checkRange(f.getInternships(), "Internships", 0, MAX_INTERNSHIPS));
        add(errors, Validators.checkRange(f.getWorkExperienceMonths(), "Work experience (months)", 0, MAX_WORK_MONTHS));
        add(errors, Validators.checkRange(f.getCommunication(), "Communication rating", 0, 10));
        add(errors, Validators.checkRange(f.getExtracurricular(), "Extracurricular rating", 0, 10));
        return errors;
    }

    /** Validate and save a profile, then record the new readiness result. */
    @PreAuthorize("hasRole('STUDENT')")
    @Transactional
    public Result<Student> updateProfile(Integer studentId, ProfileForm f, boolean firstTime) {
        List<String> errors = validate(f);
        if (!errors.isEmpty()) {
            return Result.fail(errors);
        }
        Student s = require(studentId);
        List<String> skills = f.getSkills();
        List<String> learning = f.getLearning().stream().filter(x -> !skills.contains(x)).toList();
        s.setName(Validators.clean(f.getName()));
        s.setPhone(Validators.cleanPhone(f.getPhone()));
        s.setCity(Validators.clean(f.getCity()));
        s.setPortfolioUrl(f.getPortfolioUrl() == null ? "" : f.getPortfolioUrl().strip());
        s.setUniversity(Validators.clean(f.getUniversity()));
        s.setDegree(f.getDegree());
        s.setDepartment(f.getDepartment());
        s.setGraduationYear(f.getGraduationYear());
        s.setCgpaScale(f.getCgpaScale());
        s.setCgpa(Math.round(f.getCgpa() * 100.0) / 100.0);
        s.setTargetCareer(f.getTargetCareer());
        s.setSkills(Csv.join(skills));
        s.setLearningSkills(Csv.join(learning));
        s.setCertifications(Csv.join(f.certificationList()));
        s.setProjects(f.getProjects());
        s.setInternships(f.getInternships());
        s.setWorkExperienceMonths(f.getWorkExperienceMonths());
        s.setCommunication(f.getCommunication());
        s.setExtracurricular(f.getExtracurricular());
        students.saveAndFlush(s);
        activity.record(s, firstTime ? "Profile created" : "Profile updated",
                String.format(Locale.ROOT, "CGPA %.2f, %d skills, target %s", s.getCgpa(), skills.size(), s.getTargetCareer()));
        readiness.record(s);
        reports.invalidate();
        return Result.ok(s);
    }

    private static void add(List<String> errors, String e) {
        if (e != null) {
            errors.add(e);
        }
    }
}
