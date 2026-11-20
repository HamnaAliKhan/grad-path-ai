package ai.gradpath.domain;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/** Helpers for the comma-separated list columns and UTC timestamps used by the entities. */
public final class Csv {

    private Csv() { }

    /** Split a stored comma-separated value into trimmed, non-empty, de-duplicated items. */
    public static List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.copyOf(new LinkedHashSet<>(Arrays.stream(value.split(","))
                .map(String::strip).filter(s -> !s.isEmpty()).toList()));
    }

    /** Join items back into the stored form (commas inside items are removed so the format stays valid). */
    public static String join(Collection<String> values) {
        if (values == null) {
            return "";
        }
        return String.join(",", new LinkedHashSet<>(values.stream()
                .map(v -> v == null ? "" : v.replace(",", " ").strip()).filter(v -> !v.isEmpty()).toList()));
    }

    /** UTC timestamp without a zone, matching how created_at values are stored. */
    public static LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
