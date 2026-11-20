package ai.gradpath.readiness;

import java.util.List;

/** Extracurricular factor (5 points), rules 47-57: half a point per rating step. */
public class ExtracurricularScorer extends FactorScorer {

    public ExtracurricularScorer() {
        super("extracurricular", "Extracurricular", 5, List.of(
                tier(47, "Rating 0", 0, 1, 0),
                tier(48, "Rating 1", 1, 2, 0.5),
                tier(49, "Rating 2", 2, 3, 1),
                tier(50, "Rating 3", 3, 4, 1.5),
                tier(51, "Rating 4", 4, 5, 2),
                tier(52, "Rating 5", 5, 6, 2.5),
                tier(53, "Rating 6", 6, 7, 3),
                tier(54, "Rating 7", 7, 8, 3.5),
                tier(55, "Rating 8", 8, 9, 4),
                tier(56, "Rating 9", 9, 10, 4.5),
                tier(57, "Rating 10", 10, INF, 5)));
    }

    @Override
    protected double measure(ProfileSnapshot p) {
        return p.extracurricular();
    }

    @Override
    protected String describe(ProfileSnapshot p, double value) {
        return "Extracurricular rating " + p.extracurricular() + " / 10";
    }

    @Override
    protected String advice(ProfileSnapshot p, double points) {
        if (points >= 4) {
            return "Active outside class - mention leadership examples in interviews.";
        }
        return "Joining a society, a competition team or organising an event shows teamwork and leadership.";
    }
}
