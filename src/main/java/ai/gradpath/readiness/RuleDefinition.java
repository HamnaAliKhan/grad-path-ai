package ai.gradpath.readiness;

/**
 * Documentation of one numbered readiness rule, shown on the "How the score works" page.
 *
 * @param number    rule number (1-60)
 * @param factor    factor or decision group, e.g. "CGPA"
 * @param condition when the rule applies, in plain language
 * @param outcome   what the rule does, e.g. "16 points"
 */
public record RuleDefinition(int number, String factor, String condition, String outcome) {
}
