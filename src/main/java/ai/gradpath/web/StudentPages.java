package ai.gradpath.web;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import ai.gradpath.config.GradPathProperties;
import ai.gradpath.domain.Student;
import ai.gradpath.reference.CareerCatalog;
import ai.gradpath.security.AuthenticatedUser;
import ai.gradpath.service.ProfileForm;
import ai.gradpath.service.StudentService;

/** Shared helpers for student controllers. */
@Component
public class StudentPages {

    private final StudentService students;
    private final GradPathProperties props;

    public StudentPages(StudentService students, GradPathProperties props) {
        this.students = students;
        this.props = props;
    }

    /** The signed-in student's entity. */
    public Student current(AuthenticatedUser me) {
        return students.require(me.id());
    }

    /** Options used by the profile form. */
    public void formOptions(Model model) {
        model.addAttribute("degrees", CareerCatalog.DEGREES);
        model.addAttribute("departments", CareerCatalog.DEPARTMENTS);
        model.addAttribute("careers", CareerCatalog.careerNames());
        model.addAttribute("skillCatalog", CareerCatalog.SKILL_CATALOG);
        model.addAttribute("skillCategories", CareerCatalog.SKILL_CATEGORIES);
        model.addAttribute("cgpaScales", GradPathProperties.CGPA_SCALES);
    }

    /** A blank or pre-filled form for a new student (default scale from configuration). */
    public ProfileForm initialForm(Student s) {
        ProfileForm f = ProfileForm.from(s);
        if (!s.isOnboarded()) {
            f.setCgpaScale(s.getCgpaScale() > 0 ? s.getCgpaScale() : props.safeDefaultScale());
            if (f.getCommunication() == null || f.getCommunication() == 0) {
                f.setCommunication(5);
            }
        }
        return f;
    }
}
