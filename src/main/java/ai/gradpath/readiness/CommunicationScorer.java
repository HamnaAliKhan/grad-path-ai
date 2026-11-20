package ai.gradpath.readiness;

import java.util.List;

/** Communication factor (10 points), rules 37-46. Ratings 0-1 mean "no meaningful rating yet". */
public class CommunicationScorer extends FactorScorer {

    public CommunicationScorer() {
        super("communication", "Communication", 10, List.of(
                tier(37, "Rating 0 or 1 (no meaningful rating yet)", 0, 2, 0),
                tier(38, "Rating 2", 2, 3, 2),
                tier(39, "Rating 3", 3, 4, 3),
                tier(40, "Rating 4", 4, 5, 4),
                tier(41, "Rating 5", 5, 6, 5),
                tier(42, "Rating 6", 6, 7, 6),
                tier(43, "Rating 7", 7, 8, 7),
                tier(44, "Rating 8", 8, 9, 8),
                tier(45, "Rating 9", 9, 10, 9),
                tier(46, "Rating 10", 10, INF, 10)));
    }

    @Override
    protected double measure(ProfileSnapshot p) {
        return p.communication();
    }

    @Override
    protected String describe(ProfileSnapshot p, double value) {
        return "Communication rating " + p.communication() + " / 10";
    }

    @Override
    protected String advice(ProfileSnapshot p, double points) {
        if (p.communication() <= 1) {
            return "Add an honest communication self-rating (2-10) to your profile.";
        }
        if (points >= 8) {
            return "Strong communication. Use it in interviews and project presentations.";
        }
        return "Mock interviews and short project pitches raise this quickly - each rating point is one point.";
    }
}
