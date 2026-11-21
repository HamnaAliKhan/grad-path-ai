package ai.gradpath.service.assistant;

import java.util.List;

/**
 * Anonymous profile context used to personalise answers. It deliberately contains NO name, email, phone number,
 * city or university, so it is safe to include in a request to an external AI provider.
 */
public record AssistantContext(String targetCareer, double score, String category, boolean preliminary,
                               List<String> weakestFactors, List<String> missingSkills, int skillMatch,
                               int applications, int goalProgress) {

    public AssistantContext {
        weakestFactors = List.copyOf(weakestFactors);
        missingSkills = List.copyOf(missingSkills);
    }

    /** One-line summary for the AI system prompt. */
    public String summary() {
        return "Target career: " + targetCareer + "; rule-based readiness score " + Math.round(score) + "/100 ("
                + category + (preliminary ? ", preliminary" : "") + "); weakest factors: "
                + (weakestFactors.isEmpty() ? "none" : String.join(", ", weakestFactors))
                + "; top missing skills: " + (missingSkills.isEmpty() ? "none" : String.join(", ", missingSkills))
                + "; skill match with target career " + skillMatch + "%; applications sent: " + applications
                + "; goal progress " + goalProgress + "%.";
    }
}
