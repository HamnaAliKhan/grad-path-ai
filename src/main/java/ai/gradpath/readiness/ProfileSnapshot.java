package ai.gradpath.readiness;

import java.util.List;

import ai.gradpath.domain.ProfileCompletion;
import ai.gradpath.domain.Student;

/**
 * The inputs the readiness rules use - nothing else. Protected or sensitive traits (gender, age, ethnicity,
 * religion, disability, IQ, contact details ...) are deliberately absent, so they cannot influence a score.
 *
 * <p>A saved {@link Student} and an unsaved "what-if" variation are both represented by this record,
 * so the what-if simulator scores changes without touching the database.
 */
public record ProfileSnapshot(
        double cgpa,
        double cgpaScale,
        int projects,
        int internships,
        int workExperienceMonths,
        int confidentSkills,
        int learningSkills,
        int certifications,
        int communication,
        int extracurricular,
        int profileCompletion,
        List<String> missingInformation) {

    public ProfileSnapshot {
        missingInformation = missingInformation == null ? List.of() : List.copyOf(missingInformation);
    }

    /** Snapshot of a saved student profile. */
    public static ProfileSnapshot of(Student s) {
        ProfileCompletion completion = s.profileCompletion();
        return new ProfileSnapshot(s.getCgpa(), s.getCgpaScale(), s.getProjects(), s.getInternships(),
                s.getWorkExperienceMonths(), s.skillList().size(), s.learningList().size(), s.certList().size(),
                s.getCommunication(), s.getExtracurricular(), completion.percent(), completion.missing());
    }

    public ProfileSnapshot withCgpa(double v) {
        return new ProfileSnapshot(v, cgpaScale, projects, internships, workExperienceMonths, confidentSkills,
                learningSkills, certifications, communication, extracurricular, profileCompletion, missingInformation);
    }

    public ProfileSnapshot withProjects(int v) {
        return new ProfileSnapshot(cgpa, cgpaScale, v, internships, workExperienceMonths, confidentSkills,
                learningSkills, certifications, communication, extracurricular, profileCompletion, missingInformation);
    }

    public ProfileSnapshot withInternships(int v) {
        return new ProfileSnapshot(cgpa, cgpaScale, projects, v, workExperienceMonths, confidentSkills,
                learningSkills, certifications, communication, extracurricular, profileCompletion, missingInformation);
    }

    public ProfileSnapshot withWorkExperienceMonths(int v) {
        return new ProfileSnapshot(cgpa, cgpaScale, projects, internships, v, confidentSkills,
                learningSkills, certifications, communication, extracurricular, profileCompletion, missingInformation);
    }

    public ProfileSnapshot withSkills(int confident, int learning) {
        return new ProfileSnapshot(cgpa, cgpaScale, projects, internships, workExperienceMonths, confident,
                learning, certifications, communication, extracurricular, profileCompletion, missingInformation);
    }

    public ProfileSnapshot withCertifications(int v) {
        return new ProfileSnapshot(cgpa, cgpaScale, projects, internships, workExperienceMonths, confidentSkills,
                learningSkills, v, communication, extracurricular, profileCompletion, missingInformation);
    }

    public ProfileSnapshot withCommunication(int v) {
        return new ProfileSnapshot(cgpa, cgpaScale, projects, internships, workExperienceMonths, confidentSkills,
                learningSkills, certifications, v, extracurricular, profileCompletion, missingInformation);
    }

    public ProfileSnapshot withExtracurricular(int v) {
        return new ProfileSnapshot(cgpa, cgpaScale, projects, internships, workExperienceMonths, confidentSkills,
                learningSkills, certifications, communication, v, profileCompletion, missingInformation);
    }

    public ProfileSnapshot withCompletion(int percent, List<String> missing) {
        return new ProfileSnapshot(cgpa, cgpaScale, projects, internships, workExperienceMonths, confidentSkills,
                learningSkills, certifications, communication, extracurricular, percent, missing);
    }
}
