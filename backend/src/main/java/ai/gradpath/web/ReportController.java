package ai.gradpath.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ai.gradpath.domain.Job;
import ai.gradpath.domain.JobApplication;
import ai.gradpath.reference.CareerCatalog;
import ai.gradpath.service.ApplicationService;
import ai.gradpath.service.JobService;
import ai.gradpath.service.ReportService;
import ai.gradpath.service.ReportService.StudentRow;

/** Reports & Analytics with privacy-preserving CSV downloads. */
@Controller
public class ReportController {

    private final ReportService reports;
    private final JobService jobs;
    private final ApplicationService applications;

    public ReportController(ReportService reports, JobService jobs, ApplicationService applications) {
        this.reports = reports;
        this.jobs = jobs;
        this.applications = applications;
    }

    @GetMapping("/officer/reports")
    public String page(@RequestParam(defaultValue = "true") boolean demo, Model model) {
        ReportService.Snapshot snap = reports.snapshot(demo);
        List<StudentRow> rows = snap.students();
        List<ReportService.DepartmentRow> depts = ReportService.departments(rows);
        Map<String, Long> cats = ReportService.categoryCounts(rows);
        List<Job> allJobs = jobs.all();
        List<JobApplication> apps = applications.all().stream().filter(a -> demo || !a.getStudent().isDemo()).toList();
        Map<String, Long> perJob = new LinkedHashMap<>();
        Map<Integer, Long> counts = new LinkedHashMap<>();
        apps.forEach(a -> counts.merge(a.getJob().getId(), 1L, Long::sum));
        allJobs.stream().filter(j -> counts.containsKey(j.getId()))
                .sorted((a, b) -> Long.compare(counts.get(b.getId()), counts.get(a.getId())))
                .limit(12)
                .forEach(j -> perJob.put(j.getTitle() + " (#" + j.getId() + ")", counts.get(j.getId())));
        Map<String, long[]> byCategory = new LinkedHashMap<>();
        CareerCatalog.careerNames().forEach(c -> byCategory.put(c, new long[2]));
        allJobs.forEach(j -> byCategory.computeIfAbsent(j.getCategory(), k -> new long[2])[j.isOpen() ? 0 : 1]++);

        model.addAttribute("includeDemo", demo);
        model.addAttribute("snapshot", snap);
        model.addAttribute("rows", rows);
        model.addAttribute("depts", depts);
        model.addAttribute("meanScore", ReportService.average(rows, StudentRow::score));
        model.addAttribute("medianScore", ReportService.median(rows));
        model.addAttribute("avgCgpa", ReportService.average(rows, StudentRow::cgpa4));
        model.addAttribute("totalApplications", apps.size());
        model.addAttribute("openJobs", allJobs.stream().filter(Job::isOpen).count());
        model.addAttribute("totalJobs", allJobs.size());
        model.addAttribute("charts", Map.of(
                "donut", ChartFactory.readinessDonut(cats, "Readiness category distribution"),
                "source", ChartFactory.readinessBySource(rows),
                "cgpa", ChartFactory.cgpaHistogram(rows),
                "dept", ChartFactory.departmentBar(depts),
                "internship", ChartFactory.internshipPie(rows),
                "gaps", ChartFactory.skillGaps(snap.gaps()),
                "perJob", ChartFactory.horizontalCounts(perJob, "Applications per listing", ChartFactory.VIOLET, "Applications"),
                "status", ChartFactory.statusBar(ReportService.statusCounts(apps)),
                "activity", ChartFactory.jobActivity(byCategory)));
        model.addAttribute("insights", Map.of(
                "donut", ReportService.readinessInsight(rows),
                "source", ReportService.sourceInsight(),
                "cgpa", ReportService.cgpaInsight(rows),
                "dept", ReportService.departmentInsight(depts),
                "internship", ReportService.internshipInsight(rows),
                "gaps", ReportService.gapInsight(snap.gaps()),
                "perJob", ReportService.applicationsInsight(perJob)));
        return "officer/reports";
    }

    @GetMapping("/officer/reports/students.csv")
    public ResponseEntity<byte[]> studentsCsv(@RequestParam(defaultValue = "true") boolean demo) {
        return ApplicationReviewController.csvResponse(ReportService.studentsCsv(reports.snapshot(demo).students()),
                "gradpath_student_report.csv");
    }

    @GetMapping("/officer/reports/skill-gaps.csv")
    public ResponseEntity<byte[]> gapsCsv(@RequestParam(defaultValue = "true") boolean demo) {
        return ApplicationReviewController.csvResponse(ReportService.gapsCsv(reports.snapshot(demo).gaps()),
                "gradpath_skill_gaps.csv");
    }

    @GetMapping("/officer/reports/departments.csv")
    public ResponseEntity<byte[]> departmentsCsv(@RequestParam(defaultValue = "true") boolean demo) {
        return ApplicationReviewController.csvResponse(
                ReportService.departmentsCsv(ReportService.departments(reports.snapshot(demo).students())),
                "gradpath_departments.csv");
    }
}
