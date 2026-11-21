package ai.gradpath.readiness;

import java.util.List;

/** Completed-projects factor (20 points), rules 10-16. */
public class ProjectScorer extends FactorScorer {

    public ProjectScorer() {
        super("projects", "Projects", 20, List.of(
                tier(10, "Zero projects", 0, 1, 0),
                tier(11, "One project", 1, 2, 4),
                tier(12, "Two projects", 2, 3, 8),
                tier(13, "Three projects", 3, 4, 12),
                tier(14, "Four projects", 4, 5, 16),
                tier(15, "Five or six projects", 5, 7, 18),
                tier(16, "Seven or more projects", 7, INF, 20)));
    }

    @Override
    protected double measure(ProfileSnapshot p) {
        return p.projects();
    }

    @Override
    protected String describe(ProfileSnapshot p, double value) {
        return p.projects() + " completed project" + (p.projects() == 1 ? "" : "s");
    }

    @Override
    protected String advice(ProfileSnapshot p, double points) {
        if (points >= 16) {
            return "A strong portfolio. Keep READMEs and demos up to date.";
        }
        return "Each additional completed project (up to four) adds 4 points. Publish them with a clear README.";
    }
}
