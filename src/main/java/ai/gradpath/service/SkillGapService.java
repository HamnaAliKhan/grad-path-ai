package ai.gradpath.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import ai.gradpath.domain.Student;
import ai.gradpath.reference.Career;
import ai.gradpath.reference.CareerCatalog;

/** Compare a student's skills with illustrative career requirements and with job listings. */
@Service
public class SkillGapService {

    public static final String MATCHED = "Matched";
    public static final String LEARNING = "Learning";
    public static final String MISSING = "Missing";

    /** One required skill and the student's status for it. */
    public record Row(String skill, int importance, String status, String tip) {
        public String importanceLabel() {
            return importance >= 3 ? "High" : importance == 2 ? "Medium" : "Low";
        }
    }

    /** Skill-gap analysis for one career. */
    public record Analysis(Career career, int match, List<Row> rows, List<Row> priorities, Map<String, Integer> counts) {
        public List<String> skillsWithStatus(String status) {
            return rows.stream().filter(r -> r.status().equals(status)).map(Row::skill).toList();
        }
    }

    /** Job skill match details. */
    public record JobMatch(int percent, List<String> matched, List<String> learning, List<String> missing) { }

    /**
     * Weighted career match: matched skills count fully, skills being learned count half, missing skills zero;
     * more important skills weigh more.
     */
    public Analysis analyze(Student student, String careerName) {
        Career c = CareerCatalog.career(careerName)
                .orElseThrow(() -> new NotFoundException("Unknown career: " + careerName));
        Set<String> have = new HashSet<>(student.skillList());
        Set<String> learning = new HashSet<>(student.learningList());
        List<Row> rows = new ArrayList<>();
        double earned = 0;
        for (Map.Entry<String, Integer> req : c.skills().entrySet()) {
            String skill = req.getKey();
            int importance = req.getValue();
            if (have.contains(skill)) {
                rows.add(new Row(skill, importance, MATCHED, "Keep it sharp - show it in a portfolio project."));
                earned += importance;
            } else if (learning.contains(skill)) {
                rows.add(new Row(skill, importance, LEARNING, CareerCatalog.tipFor(skill, true)));
                earned += 0.5 * importance;
            } else {
                rows.add(new Row(skill, importance, MISSING, CareerCatalog.tipFor(skill, false)));
            }
        }
        List<Row> priorities = rows.stream().filter(r -> !MATCHED.equals(r.status()))
                .sorted(Comparator.comparingInt((Row r) -> -r.importance()).thenComparing(r -> !MISSING.equals(r.status())))
                .limit(3).toList();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String s : List.of(MATCHED, LEARNING, MISSING)) {
            counts.put(s, (int) rows.stream().filter(r -> r.status().equals(s)).count());
        }
        return new Analysis(c, (int) Math.round(100 * earned / c.totalImportance()), rows, priorities, counts);
    }

    /** Job skill match: required skills that are confident count 1, being learned 0.5, otherwise 0. */
    public JobMatch matchJob(Student student, List<String> jobSkills) {
        Set<String> have = new HashSet<>(student.skillList());
        Set<String> learning = new HashSet<>(student.learningList());
        List<String> matched = new ArrayList<>();
        List<String> learn = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String s : jobSkills) {
            if (have.contains(s)) {
                matched.add(s);
            } else if (learning.contains(s)) {
                learn.add(s);
            } else {
                missing.add(s);
            }
        }
        int pct = jobSkills.isEmpty() ? 0
                : (int) Math.round(100 * (matched.size() + 0.5 * learn.size()) / jobSkills.size());
        return new JobMatch(pct, matched, learn, missing);
    }
}
