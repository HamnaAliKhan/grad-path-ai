package ai.gradpath.reference;

import java.util.List;
import java.util.Map;

/**
 * One illustrative career path. {@code skills} maps each required skill to its importance (3 = high, 2 = medium,
 * 1 = low) in display order. These are examples, not verified employer requirements.
 */
public record Career(String name, String slug, String description, List<String> responsibilities,
                     Map<String, Integer> skills, List<String> preparation, List<String> projects) {

    public List<String> skillNames() {
        return List.copyOf(skills.keySet());
    }

    public int totalImportance() {
        return skills.values().stream().mapToInt(Integer::intValue).sum();
    }
}
