package ai.gradpath.readiness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tier boundaries, decision rules 58-60, score limits and validation of the 60-rule readiness engine. */
class ReadinessRuleEngineTest {

    private final ReadinessRuleEngine engine = new ReadinessRuleEngine();

    /** A complete (100%) profile with every value at zero unless changed. */
    static ProfileSnapshot base() {
        return new ProfileSnapshot(0, 4.0, 0, 0, 0, 0, 0, 0, 0, 0, 100, List.of());
    }

    private FactorResult factor(ProfileSnapshot p, String key) {
        return engine.evaluate(p).factor(key);
    }

    @Test
    @DisplayName("exactly 60 rules, numbered 1..60, and factor weights total 100")
    void sixtyRules() {
        List<RuleDefinition> rules = engine.rules();
        assertThat(rules).hasSize(60);
        assertThat(rules.stream().map(RuleDefinition::number).toList())
                .containsExactlyElementsOf(IntStream.rangeClosed(1, 60).boxed().toList());
        assertThat(engine.scorers().stream().mapToDouble(FactorScorer::maxPoints).sum()).isEqualTo(100.0);
    }

    @Test
    void tiersAreContiguousAndNonOverlapping() {
        for (FactorScorer s : engine.scorers()) {
            List<TierRule> tiers = s.tiers();
            assertThat(tiers.get(0).lower()).isZero();
            for (int i = 1; i < tiers.size(); i++) {
                assertThat(tiers.get(i).lower()).as(s.name()).isEqualTo(tiers.get(i - 1).upper());
            }
            assertThat(tiers.get(tiers.size() - 1).upper()).isInfinite();
        }
    }

    @Nested
    class Cgpa {
        @ParameterizedTest(name = "CGPA {0}/{1} -> rule {2}, {3} points")
        @CsvSource({
            "1.99,4.0,1,0", "2.0,4.0,2,4", "2.39,4.0,2,4", "2.4,4.0,3,8", "2.8,4.0,4,12", "3.19,4.0,4,12",
            "3.2,4.0,5,16", "3.39,4.0,5,16", "3.4,4.0,6,20", "3.6,4.0,7,24", "3.79,4.0,7,24", "3.8,4.0,8,27",
            "3.99,4.0,8,27", "4.0,4.0,9,30", "0,4.0,1,0",
            "8.5,10.0,6,20", "5.0,5.0,9,30", "4.75,5.0,8,27", "4.25,5.0,6,20"})
        void tiers(double cgpa, double scale, int rule, double points) {
            ProfileSnapshot p = new ProfileSnapshot(cgpa, scale, 0, 0, 0, 0, 0, 0, 0, 0, 100, List.of());
            FactorResult f = factor(p, "cgpa");
            assertThat(f.appliedRules()).containsExactly(rule);
            assertThat(f.points()).isEqualTo(points);
        }
    }

    @ParameterizedTest(name = "{0} projects -> rule {1}, {2} points")
    @CsvSource({"0,10,0", "1,11,4", "2,12,8", "3,13,12", "4,14,16", "5,15,18", "6,15,18", "7,16,20", "40,16,20"})
    void projects(int n, int rule, double points) {
        FactorResult f = factor(base().withProjects(n), "projects");
        assertThat(f.appliedRules()).containsExactly(rule);
        assertThat(f.points()).isEqualTo(points);
    }

    @ParameterizedTest(name = "{0} internships -> rule {1}, {2} points")
    @CsvSource({"0,17,0", "1,18,5", "2,19,9", "3,20,12", "4,21,14", "5,22,15", "9,22,15"})
    void internships(int n, int rule, double points) {
        FactorResult f = factor(base().withInternships(n), "experience");
        assertThat(f.appliedRules()).containsExactly(rule);
        assertThat(f.points()).isEqualTo(points);
    }

    @ParameterizedTest(name = "{0} confident + {1} learning -> rule {2}, {3} points")
    @CsvSource({"0,0,23,0", "0,1,24,2", "1,0,24,2", "1,1,25,4", "2,0,25,4", "2,1,26,6", "3,1,27,8", "4,1,28,10",
                "5,1,29,12", "6,1,30,14", "7,1,30,14", "7,2,31,15", "8,0,31,15", "20,5,31,15"})
    void skills(int confident, int learning, int rule, double points) {
        FactorResult f = factor(base().withSkills(confident, learning), "skills");
        assertThat(f.appliedRules()).containsExactly(rule);
        assertThat(f.points()).isEqualTo(points);
    }

    @ParameterizedTest(name = "{0} certifications -> rule {1}, {2} points")
    @CsvSource({"0,32,0", "1,33,1", "2,34,3", "3,35,4", "4,36,5", "12,36,5"})
    void certifications(int n, int rule, double points) {
        FactorResult f = factor(base().withCertifications(n), "certifications");
        assertThat(f.appliedRules()).containsExactly(rule);
        assertThat(f.points()).isEqualTo(points);
    }

    @ParameterizedTest(name = "communication {0} -> rule {1}, {2} points")
    @CsvSource({"0,37,0", "1,37,0", "2,38,2", "3,39,3", "4,40,4", "5,41,5", "6,42,6", "7,43,7", "8,44,8", "9,45,9", "10,46,10"})
    void communication(int rating, int rule, double points) {
        FactorResult f = factor(base().withCommunication(rating), "communication");
        assertThat(f.appliedRules()).containsExactly(rule);
        assertThat(f.points()).isEqualTo(points);
    }

    @ParameterizedTest(name = "extracurricular {0} -> rule {1}, {2} points")
    @CsvSource({"0,47,0", "1,48,0.5", "2,49,1", "3,50,1.5", "4,51,2", "5,52,2.5", "6,53,3", "7,54,3.5", "8,55,4",
                "9,56,4.5", "10,57,5"})
    void extracurricular(int rating, int rule, double points) {
        FactorResult f = factor(base().withExtracurricular(rating), "extracurricular");
        assertThat(f.appliedRules()).containsExactly(rule);
        assertThat(f.points()).isEqualTo(points);
    }

    @Nested
    class Rule58 {
        @Test
        void twelveMonthsWorkWithoutInternshipGivesFivePoints() {
            FactorResult f = factor(base().withWorkExperienceMonths(12), "experience");
            assertThat(f.points()).isEqualTo(5);
            assertThat(f.appliedRules()).containsExactly(17, 58);
        }

        @Test
        void elevenMonthsIsNotEnough() {
            FactorResult f = factor(base().withWorkExperienceMonths(11), "experience");
            assertThat(f.points()).isZero();
            assertThat(f.appliedRules()).containsExactly(17);
        }

        @Test
        void doesNotStackWithInternshipPoints() {
            FactorResult f = factor(base().withInternships(1).withWorkExperienceMonths(36), "experience");
            assertThat(f.points()).isEqualTo(5);
            assertThat(f.appliedRules()).containsExactly(18);
            FactorResult two = factor(base().withInternships(2).withWorkExperienceMonths(36), "experience");
            assertThat(two.points()).isEqualTo(9);
        }
    }

    @Nested
    class Rule59 {
        @Test
        void below50PercentIsPreliminaryWithMissingItems() {
            ReadinessResult r = engine.evaluate(base().withCompletion(49, List.of("Phone number", "City")));
            assertThat(r.preliminary()).isTrue();
            assertThat(r.missingInformation()).containsExactly("Phone number", "City");
            assertThat(r.appliedRules()).contains(59);
        }

        @Test
        void fiftyPercentIsNotPreliminary() {
            ReadinessResult r = engine.evaluate(base().withCompletion(50, List.of("City")));
            assertThat(r.preliminary()).isFalse();
            assertThat(r.missingInformation()).isEmpty();
            assertThat(r.appliedRules()).doesNotContain(59);
        }
    }

    @Nested
    class Rule60AndLimits {
        @Test
        void minimumScoreIsZeroNeedsImprovement() {
            ReadinessResult r = engine.evaluate(base());
            assertThat(r.score()).isZero();
            assertThat(r.category()).isEqualTo(ReadinessCategory.NEEDS_IMPROVEMENT);
            assertThat(r.appliedRules()).contains(60);
        }

        @Test
        void maximumScoreIs100JobReady() {
            ProfileSnapshot best = new ProfileSnapshot(4.0, 4.0, 50, 20, 600, 30, 10, 15, 10, 10, 100, List.of());
            ReadinessResult r = engine.evaluate(best);
            assertThat(r.score()).isEqualTo(100.0);
            assertThat(r.category()).isEqualTo(ReadinessCategory.JOB_READY);
            assertThat(r.calculation()).endsWith("= 100");
        }

        @ParameterizedTest(name = "score {0} -> {1}")
        @CsvSource({"0,NEEDS_IMPROVEMENT", "39.5,NEEDS_IMPROVEMENT", "40,DEVELOPING", "69.5,DEVELOPING", "70,JOB_READY", "100,JOB_READY"})
        void categoryBoundaries(double score, ReadinessCategory expected) {
            assertThat(ReadinessCategory.forScore(score)).isEqualTo(expected);
        }

        @Test
        void allThreeCategoriesFromRealProfiles() {
            // 12 + 8 + 5 + 6 + 1 + 5 + 2 = 39 -> Needs Improvement
            ProfileSnapshot low = new ProfileSnapshot(2.8, 4.0, 2, 1, 0, 3, 0, 1, 5, 4, 100, List.of());
            // 16 + 12 + 5 + 8 + 1 + 7 + 3 = 52 -> Developing
            ProfileSnapshot mid = new ProfileSnapshot(3.2, 4.0, 3, 1, 0, 4, 0, 1, 7, 6, 100, List.of());
            // 24 + 16 + 9 + 12 + 3 + 8 + 4 = 76 -> Job Ready
            ProfileSnapshot high = new ProfileSnapshot(3.6, 4.0, 4, 2, 0, 6, 0, 2, 8, 8, 100, List.of());
            assertThat(engine.evaluate(low).score()).isEqualTo(39.0);
            assertThat(engine.evaluate(low).category()).isEqualTo(ReadinessCategory.NEEDS_IMPROVEMENT);
            assertThat(engine.evaluate(mid).score()).isEqualTo(52.0);
            assertThat(engine.evaluate(mid).category()).isEqualTo(ReadinessCategory.DEVELOPING);
            assertThat(engine.evaluate(high).score()).isEqualTo(76.0);
            assertThat(engine.evaluate(high).category()).isEqualTo(ReadinessCategory.JOB_READY);
        }

        @Test
        void exactlyOneTierRulePerFactor() {
            ReadinessResult r = engine.evaluate(new ProfileSnapshot(3.3, 4.0, 3, 0, 0, 4, 2, 1, 7, 5, 80, List.of()));
            assertThat(r.factors()).hasSize(7).allSatisfy(f -> assertThat(f.appliedRules()).hasSize(1));
            assertThat(r.score()).isEqualTo(r.factors().stream().mapToDouble(FactorResult::points).sum());
        }

        @Test
        void deterministic() {
            ProfileSnapshot p = new ProfileSnapshot(3.35, 4.0, 3, 1, 4, 5, 3, 1, 7, 6, 92, List.of());
            assertThat(engine.evaluate(p)).isEqualTo(engine.evaluate(p));
        }
    }

    @Nested
    class Validation {
        @Test
        void cgpaAboveScaleIsRejected() {
            assertThatThrownBy(() -> engine.evaluate(base().withCgpa(4.01)))
                    .isInstanceOf(InvalidProfileException.class).hasMessageContaining("CGPA must be between 0 and 4");
        }

        @Test
        void negativeValuesAreRejected() {
            assertThatThrownBy(() -> engine.evaluate(base().withCgpa(-1))).isInstanceOf(InvalidProfileException.class);
            assertThatThrownBy(() -> engine.evaluate(base().withProjects(-1)))
                    .hasMessage("Completed projects cannot be negative.");
            assertThatThrownBy(() -> engine.evaluate(base().withInternships(-2))).isInstanceOf(InvalidProfileException.class);
            assertThatThrownBy(() -> engine.evaluate(base().withSkills(-1, 0))).isInstanceOf(InvalidProfileException.class);
        }

        @Test
        void ratingsOutside0To10AreRejected() {
            assertThatThrownBy(() -> engine.evaluate(base().withCommunication(11)))
                    .hasMessage("Communication rating must be between 0 and 10.");
            assertThatThrownBy(() -> engine.evaluate(base().withExtracurricular(-1))).isInstanceOf(InvalidProfileException.class);
        }

        @Test
        void invalidScaleAndNaNAreRejected() {
            assertThatThrownBy(() -> engine.evaluate(new ProfileSnapshot(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100, List.of())))
                    .isInstanceOf(InvalidProfileException.class);
            assertThatThrownBy(() -> engine.evaluate(base().withCgpa(Double.NaN))).isInstanceOf(InvalidProfileException.class);
        }

        @Test
        void completionOutsideRangeIsRejected() {
            assertThatThrownBy(() -> engine.evaluate(base().withCompletion(101, List.of())))
                    .isInstanceOf(InvalidProfileException.class);
        }
    }
}
