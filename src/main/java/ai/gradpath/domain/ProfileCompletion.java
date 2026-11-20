package ai.gradpath.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * How complete a student's profile is: a percentage and the labels of the items still missing.
 * Used for the dashboard indicator and by readiness rule 59 ("Preliminary" results).
 */
public record ProfileCompletion(int percent, List<String> missing) {

    /** Evaluate the 13 profile items. */
    public static ProfileCompletion of(Student s) {
        Map<String, Boolean> checks = new LinkedHashMap<>();
        checks.put("Full name", s.getName() != null && !s.getName().isBlank());
        checks.put("Phone number", !s.getPhone().isBlank());
        checks.put("City", !s.getCity().isBlank());
        checks.put("University", !s.getUniversity().isBlank());
        checks.put("Degree", !s.getDegree().isBlank());
        checks.put("Department", !s.getDepartment().isBlank());
        checks.put("Graduation year", s.getGraduationYear() > 0);
        checks.put("CGPA", s.getCgpa() > 0);
        checks.put("Target career", !s.getTargetCareer().isBlank());
        checks.put("Confident skills", !s.skillList().isEmpty());
        checks.put("Communication rating (2 or higher)", s.getCommunication() >= 2);
        checks.put("Experience (projects, internships or work months)",
                s.getProjects() > 0 || s.getInternships() > 0 || s.getWorkExperienceMonths() > 0);
        checks.put("Certifications or portfolio link", !s.certList().isEmpty() || !s.getPortfolioUrl().isBlank());
        List<String> missing = new ArrayList<>();
        checks.forEach((label, ok) -> {
            if (!ok) {
                missing.add(label);
            }
        });
        int done = checks.size() - missing.size();
        return new ProfileCompletion((int) Math.round(100.0 * done / checks.size()), List.copyOf(missing));
    }
}
