package ai.gradpath.web;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.gradpath.readiness.ReadinessCategory;
import ai.gradpath.readiness.ReadinessFormat;
import ai.gradpath.service.FitCalculator;
import ai.gradpath.service.SkillGapService;

/** Formatting helpers available in templates as {@code @ui}. Keeps presentation logic out of the templates. */
@Component("ui")
public class ViewHelper {

    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm 'UTC'", Locale.ENGLISH);

    private final ObjectMapper json;

    public ViewHelper(ObjectMapper json) {
        this.json = json;
    }

    /** ISO-8601 UTC timestamp for &lt;time datetime&gt; (converted to the viewer's local time by app.js). */
    public String iso(LocalDateTime t) {
        return t == null ? "" : t.atOffset(ZoneOffset.UTC).toString();
    }

    /** Fallback text when JavaScript is off. */
    public String time(LocalDateTime t) {
        return t == null ? "" : SHORT.format(t);
    }

    public String points(double v) {
        return ReadinessFormat.points(v);
    }

    public String one(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    public String two(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    /** Slider maximum: the default limit, or the student's own value when it is larger. */
    public int atLeast(int limit, int value) {
        return Math.max(limit, value);
    }

    public int clampPct(double v) {
        return (int) Math.max(0, Math.min(100, Math.round(v)));
    }

    public String categoryKind(String label) {
        return ReadinessCategory.fromLabel(label).badge();
    }

    public String statusKind(String status) {
        return switch (status == null ? "" : status) {
            case "Submitted" -> "neutral";
            case "Under review" -> "info";
            case "Shortlisted", "Interview" -> "violet";
            case "Offer" -> "success";
            case "Not selected" -> "danger";
            default -> "neutral";
        };
    }

    public String skillKind(String status) {
        return switch (status == null ? "" : status) {
            case SkillGapService.MATCHED -> "success";
            case SkillGapService.LEARNING -> "warn";
            default -> "danger";
        };
    }

    public String matchKind(int pct) {
        return pct >= 70 ? "success" : pct >= 40 ? "info" : "neutral";
    }

    public FitCalculator.FitLabel fit(int score) {
        return FitCalculator.FitLabel.of(score);
    }

    public String scale(double s) {
        return ReadinessFormat.points(s) + (s == Math.rint(s) ? ".0" : "");
    }

    /** JSON for data attributes (Thymeleaf escapes the attribute value). */
    public String json(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static final java.util.Map<String, String> ICONS = java.util.Map.ofEntries(
        java.util.Map.entry("dashboard", "M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z"),
        java.util.Map.entry("person", "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"),
        java.util.Map.entry("speed", "M20.38 8.57l-1.23 1.85a8 8 0 0 1-.22 7.58H5.07A8 8 0 0 1 15.58 6.85l1.85-1.23A10 10 0 0 0 3.35 19a2 2 0 0 0 1.72 1h13.85a2 2 0 0 0 1.74-1 10 10 0 0 0-.27-10.44zm-9.79 6.84a2 2 0 0 0 2.83 0l5.66-8.49-8.49 5.66a2 2 0 0 0 0 2.83z"),
        java.util.Map.entry("compare", "M9.01 14H2v2h7.01v3L13 15l-3.99-4v3zm5.98-1v-3H22V8h-7.01V5L11 9l3.99 4z"),
        java.util.Map.entry("explore", "M12 10.9c-.61 0-1.1.49-1.1 1.1s.49 1.1 1.1 1.1c.61 0 1.1-.49 1.1-1.1s-.49-1.1-1.1-1.1zM12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm2.19 12.19L6 18l3.81-8.19L18 6l-3.81 8.19z"),
        java.util.Map.entry("work", "M20 6h-4V4c0-1.11-.89-2-2-2h-4c-1.11 0-2 .89-2 2v2H4c-1.11 0-1.99.89-1.99 2L2 19c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2zm-6 0h-4V4h4v2z"),
        java.util.Map.entry("assignment", "M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"),
        java.util.Map.entry("description", "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"),
        java.util.Map.entry("flag", "M14.4 6L14 4H5v17h2v-7h5.6l.4 2h7V6z"),
        java.util.Map.entry("mail", "M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"),
        java.util.Map.entry("chat", "M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM6 9h12v2H6V9zm8 5H6v-2h8v2zm4-6H6V6h12v2z"),
        java.util.Map.entry("group", "M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"),
        java.util.Map.entry("chart", "M5 9.2h3V19H5zM10.6 5h2.8v14h-2.8zm5.6 8H19v6h-2.8z"),
        java.util.Map.entry("model", "M21 8c-1.45 0-2.26 1.44-1.93 2.51l-3.55 3.56c-.3-.09-.74-.09-1.04 0l-2.55-2.55C12.27 10.45 11.46 9 10 9c-1.45 0-2.27 1.44-1.93 2.52l-4.56 4.55C2.44 15.74 1 16.55 1 18c0 1.1.9 2 2 2 1.45 0 2.26-1.44 1.93-2.51l4.55-4.56c.3.09.74.09 1.04 0l2.55 2.55C12.73 16.55 13.54 18 15 18c1.45 0 2.27-1.44 1.93-2.52l3.56-3.55c1.07.33 2.51-.48 2.51-1.93 0-1.1-.9-2-2-2z"),
        java.util.Map.entry("rule", "M11 18h2v-2h-2v2zm1-16C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm0-14c-2.21 0-4 1.79-4 4h2c0-1.1.9-2 2-2s2 .9 2 2c0 2-3 1.75-3 5h2c0-2.25 3-2.5 3-5 0-2.21-1.79-4-4-4z"));

    /** A 0..1 share as a whole percentage, e.g. 0.583 -> "58%". */
    public String percent(double share) {
        return Math.round(share * 100) + "%";
    }

    /** SVG path data for a navigation icon (Material Design icon shapes). */
    public String icon(String name) {
        return ICONS.getOrDefault(name, ICONS.get("dashboard"));
    }

    public String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        StringBuilder sb = new StringBuilder();
        for (String p : name.strip().split("\\s+")) {
            if (sb.length() < 2) {
                sb.append(Character.toUpperCase(p.charAt(0)));
            }
        }
        return sb.toString();
    }
}
