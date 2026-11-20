package ai.gradpath.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.gradpath.domain.ApplicationStatus;
import ai.gradpath.domain.Job;
import ai.gradpath.domain.JobApplication;
import ai.gradpath.domain.Student;
import ai.gradpath.readiness.CgpaScorer;
import ai.gradpath.readiness.ReadinessCategory;
import ai.gradpath.readiness.ReadinessResult;
import ai.gradpath.reference.CareerCatalog;
import ai.gradpath.repository.StudentRepository;

/**
 * Officer analytics built from the records in the database (registered students and clearly labelled fictional
 * demo records). Exports contain no names, emails or phone numbers unless a screening list requires a name.
 */
@Service
public class ReportService {

    private static final long TTL_MILLIS = 15_000;

    /** One anonymised student row. CGPA is also normalised to a 4.0-equivalent so different scales compare. */
    public record StudentRow(Integer id, String name, String department, int graduationYear, double cgpa, double cgpaScale,
                             double cgpa4, int projects, int internships, int skills, String targetCareer,
                             double score, String category, boolean preliminary, boolean demo) {
        public String source() {
            return demo ? "Demo (fictional)" : "Registered";
        }
    }

    public record GapRow(String skill, int students) { }

    public record DepartmentRow(String department, int students, double averageScore, double averageCgpa4, int jobReady) { }

    public record Snapshot(List<StudentRow> students, List<GapRow> gaps) {
        public boolean isEmpty() {
            return students.isEmpty();
        }

        public long demoCount() {
            return students.stream().filter(StudentRow::demo).count();
        }
    }

    private record Cached(long at, Snapshot snapshot) { }

    private final StudentRepository students;
    private final ReadinessService readiness;
    private final SkillGapService skillGap;
    private final Map<Boolean, Cached> cache = new ConcurrentHashMap<>();

    public ReportService(StudentRepository students, ReadinessService readiness, SkillGapService skillGap) {
        this.students = students;
        this.readiness = readiness;
        this.skillGap = skillGap;
    }

    /** Rows for onboarded students plus the 10 most common career-skill gaps (cached for 15 seconds). */
    @PreAuthorize("hasRole('OFFICER')")
    @Transactional(readOnly = true)
    public Snapshot snapshot(boolean includeDemo) {
        Cached c = cache.get(includeDemo);
        if (c != null && System.currentTimeMillis() - c.at() < TTL_MILLIS) {
            return c.snapshot();
        }
        List<StudentRow> rows = new ArrayList<>();
        Map<String, Integer> gapCounter = new LinkedHashMap<>();
        for (Student st : students.listStudents(includeDemo)) {
            if (!st.isOnboarded() || st.getCgpa() > st.getCgpaScale()) {
                continue;
            }
            ReadinessResult r = readiness.evaluate(st);
            String career = CareerCatalog.resolveCareer(st.getTargetCareer(), st.getDepartment());
            skillGap.analyze(st, career).rows().stream()
                    .filter(row -> !SkillGapService.MATCHED.equals(row.status()))
                    .forEach(row -> gapCounter.merge(row.skill(), 1, Integer::sum));
            rows.add(new StudentRow(st.getId(), st.getName(), st.getDepartment().isBlank() ? "Not set" : st.getDepartment(),
                    st.getGraduationYear(), st.getCgpa(), st.getCgpaScale(),
                    Math.round(CgpaScorer.ratio(st.getCgpa(), st.getCgpaScale()) * 4.0 * 100) / 100.0,
                    st.getProjects(), st.getInternships(), st.skillList().size(), career, r.score(), r.category().label(),
                    r.preliminary(), st.isDemo()));
        }
        List<GapRow> gaps = gapCounter.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, Integer> e) -> -e.getValue()))
                .limit(10).map(e -> new GapRow(e.getKey(), e.getValue())).toList();
        Snapshot snap = new Snapshot(rows, gaps);
        cache.put(includeDemo, new Cached(System.currentTimeMillis(), snap));
        return snap;
    }

    public void invalidate() {
        cache.clear();
    }

    // ---------- aggregates ----------

    public static Map<String, Long> categoryCounts(List<StudentRow> rows) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (ReadinessCategory c : ReadinessCategory.values()) {
            out.put(c.label(), rows.stream().filter(r -> r.category().equals(c.label())).count());
        }
        return out;
    }

    public static double average(List<StudentRow> rows, java.util.function.ToDoubleFunction<StudentRow> f) {
        return rows.stream().mapToDouble(f).average().orElse(0);
    }

    public static double median(List<StudentRow> rows) {
        double[] v = rows.stream().mapToDouble(StudentRow::score).sorted().toArray();
        if (v.length == 0) {
            return 0;
        }
        return v.length % 2 == 1 ? v[v.length / 2] : (v[v.length / 2 - 1] + v[v.length / 2]) / 2;
    }

    public static List<DepartmentRow> departments(List<StudentRow> rows) {
        Map<String, List<StudentRow>> by = new TreeMap<>();
        rows.forEach(r -> by.computeIfAbsent(r.department(), k -> new ArrayList<>()).add(r));
        List<DepartmentRow> out = new ArrayList<>();
        by.forEach((d, list) -> out.add(new DepartmentRow(d, list.size(), round1(average(list, StudentRow::score)),
                round2(average(list, StudentRow::cgpa4)),
                (int) list.stream().filter(r -> r.category().equals(ReadinessCategory.JOB_READY.label())).count())));
        return out;
    }

    public static Map<String, Long> statusCounts(List<JobApplication> apps) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (String s : ApplicationStatus.labels()) {
            out.put(s, apps.stream().filter(a -> a.getStatus().equals(s)).count());
        }
        return out;
    }

    // ---------- plain-language insights ----------

    public static String readinessInsight(List<StudentRow> rows) {
        Map<String, Long> c = categoryCounts(rows);
        int n = rows.size();
        return String.format(Locale.ROOT, "Each slice is a group of students. %d%% are Needs Improvement, %d%% Developing and "
                        + "%d%% Job Ready according to the rule-based score. A larger green slice means more students meet "
                        + "most readiness thresholds - it is not a prediction of who will be hired.",
                pct(c.get("Needs Improvement"), n), pct(c.get("Developing"), n), pct(c.get("Job Ready"), n));
    }

    public static String departmentInsight(List<DepartmentRow> depts) {
        if (depts.isEmpty()) {
            return "No department data yet.";
        }
        DepartmentRow big = depts.stream().max(Comparator.comparingInt(DepartmentRow::students)).orElseThrow();
        DepartmentRow best = depts.stream().max(Comparator.comparingDouble(DepartmentRow::averageScore)).orElseThrow();
        return String.format(Locale.ROOT, "Each bar is a department; taller bars mean more students and a brighter colour "
                        + "means a higher average readiness score. %s has the most students (%d). %s has the highest "
                        + "average readiness score (%.1f).", big.department(), big.students(), best.department(),
                best.averageScore());
    }

    public static String cgpaInsight(List<StudentRow> rows) {
        long below = rows.stream().filter(r -> r.cgpa4() < 2.5).count();
        return String.format(Locale.ROOT, "This shows how grades are spread, converted to a 4.0-equivalent so different "
                        + "grading scales can be compared. The average is %.2f. %d student(s) are below 2.50 and may "
                        + "benefit from academic support.", average(rows, StudentRow::cgpa4), below);
    }

    public static String internshipInsight(List<StudentRow> rows) {
        long with = rows.stream().filter(r -> r.internships() > 0).count();
        return String.format(Locale.ROOT, "%d%% of students have completed at least one internship. If the grey slice is "
                + "large, more internship partnerships or practical projects could help.", pct(with, rows.size()));
    }

    public static String gapInsight(List<GapRow> gaps) {
        if (gaps.isEmpty()) {
            return "No common skill gaps found.";
        }
        return "Each bar counts students who are missing, or still learning, a skill listed for their target career. "
                + "The longest bar is " + gaps.get(0).skill() + " (" + gaps.get(0).students()
                + " students), so a workshop on it would help the most people.";
    }

    public static String applicationsInsight(Map<String, Long> perJob) {
        if (perJob.isEmpty()) {
            return "No applications yet.";
        }
        Map.Entry<String, Long> top = perJob.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow();
        return "Each bar is a listing and shows how many students applied. " + top.getKey() + " is the most popular with "
                + top.getValue() + " application(s). Listings with few applications may need promotion or clearer requirements.";
    }

    public static String sourceInsight() {
        return "Bars separate registered students (real accounts) from fictional demo records, so demo data is never "
                + "mistaken for real results.";
    }

    // ---------- CSV exports ----------

    /** Student report without names or contact details (pseudonymous record id only). */
    public static String studentsCsv(List<StudentRow> rows) {
        List<List<Object>> out = rows.stream().map(r -> List.<Object>of(r.id(), r.source(), r.department(), r.graduationYear(),
                r.cgpa(), r.cgpaScale(), r.cgpa4(), r.projects(), r.internships(), r.skills(), r.targetCareer(),
                r.score(), r.category(), r.preliminary() ? "Yes" : "No")).toList();
        return CsvWriter.write(List.of("Record ID", "Source", "Department", "Graduation year", "CGPA", "CGPA scale",
                "CGPA (4.0 equivalent)", "Projects", "Internships", "Confident skills", "Target career",
                "Readiness score (0-100)", "Readiness category", "Preliminary"), out);
    }

    public static String gapsCsv(List<GapRow> gaps) {
        return CsvWriter.write(List.of("Skill", "Students with gap"),
                gaps.stream().map(g -> List.<Object>of(g.skill(), g.students())).toList());
    }

    public static String departmentsCsv(List<DepartmentRow> depts) {
        return CsvWriter.write(List.of("Department", "Students", "Average readiness score", "Average CGPA (4.0 equivalent)",
                        "Job Ready students"),
                depts.stream().map(d -> List.<Object>of(d.department(), d.students(), d.averageScore(), d.averageCgpa4(),
                        d.jobReady())).toList());
    }

    /** Ranked screening list for one job: candidate names are needed for review, contact details are excluded. */
    public static String applicantsCsv(Job job, List<JobApplication> apps) {
        List<List<Object>> out = new ArrayList<>();
        int rank = 1;
        for (JobApplication a : apps) {
            out.add(List.of(rank++, a.getStudent().getName(), a.getFitScore(), FitCalculator.FitLabel.of(a.getFitScore()).label(),
                    a.getSkillMatch(), a.getResumeMatch(), a.getReadinessCategory(), a.getStatus(),
                    a.getStudent().isDemo() ? "Demo (fictional)" : "Registered"));
        }
        return CsvWriter.write(List.of("Rank", "Candidate", "Fit score", "Fit", "Skill match %", "Resume match %",
                "Readiness", "Status", "Source"), out);
    }

    private static int pct(Long part, int total) {
        return total == 0 || part == null ? 0 : (int) Math.round(100.0 * part / total);
    }

    private static int pct(long part, int total) {
        return total == 0 ? 0 : (int) Math.round(100.0 * part / total);
    }

    static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
