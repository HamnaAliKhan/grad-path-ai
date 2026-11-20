package ai.gradpath.ml;

import java.util.List;

import ai.gradpath.readiness.ProfileSnapshot;

/**
 * The five model inputs. They are a subset of what the rule engine already uses; protected or sensitive traits
 * (gender, age, ethnicity, IQ, contact details ...) are never features, so they cannot influence an estimate.
 *
 * @param cgpaRatio       CGPA divided by its grading scale (0..1), so 4-, 5- and 10-point scales are comparable
 * @param projects        completed projects
 * @param internship      1 when the student has done at least one internship, otherwise 0
 * @param communication   self-rated communication, 0..10
 * @param extracurricular self-rated extracurricular involvement, 0..10
 */
public record PlacementFeatures(double cgpaRatio, double projects, double internship, double communication,
                                double extracurricular) {

    /** Feature keys, in the order used by {@link #toArray()} and stored in the model file. */
    public static final List<String> NAMES = List.of("cgpa_ratio", "projects", "internship", "communication", "extracurricular");

    /** Human-readable feature names, same order. */
    public static final List<String> LABELS = List.of("CGPA", "Projects", "Internship", "Communication", "Extracurricular");

    public static final int COUNT = NAMES.size();

    public double[] toArray() {
        return new double[] {cgpaRatio, projects, internship, communication, extracurricular};
    }

    public static PlacementFeatures of(double[] v) {
        if (v.length != COUNT) {
            throw new IllegalArgumentException("Expected " + COUNT + " feature values but got " + v.length);
        }
        return new PlacementFeatures(v[0], v[1], v[2], v[3], v[4]);
    }

    /** Model inputs for a saved profile or an unsaved what-if variation. */
    public static PlacementFeatures of(ProfileSnapshot p) {
        double scale = p.cgpaScale() > 0 ? p.cgpaScale() : 4.0;
        double ratio = Math.max(0, Math.min(1, p.cgpa() / scale));
        return new PlacementFeatures(ratio, Math.max(0, p.projects()), p.internships() > 0 ? 1 : 0,
                Math.max(0, Math.min(10, p.communication())), Math.max(0, Math.min(10, p.extracurricular())));
    }
}
