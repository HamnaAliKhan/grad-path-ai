package ai.gradpath.web;

import java.io.IOException;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import ai.gradpath.domain.Student;
import ai.gradpath.reference.CareerCatalog;
import ai.gradpath.security.AuthenticatedUser;
import ai.gradpath.service.ActivityService;
import ai.gradpath.service.Result;
import ai.gradpath.service.ResumeService;

/** Resume Analyzer: the PDF is parsed for text in memory, analysed, and then discarded (not stored). */
@Controller
public class ResumeController {

    private final StudentPages pages;
    private final ResumeService resumes;
    private final ActivityService activity;

    public ResumeController(StudentPages pages, ResumeService resumes, ActivityService activity) {
        this.pages = pages;
        this.resumes = resumes;
        this.activity = activity;
    }

    @GetMapping("/student/resume")
    public String page(@AuthenticationPrincipal AuthenticatedUser me, Model model) {
        Student s = pages.current(me);
        model.addAttribute("careers", CareerCatalog.careerNames());
        model.addAttribute("career", CareerCatalog.resolveCareer(s.getTargetCareer(), s.getDepartment()));
        return "student/resume";
    }

    @PostMapping("/student/resume")
    public String analyze(@AuthenticationPrincipal AuthenticatedUser me, @RequestParam(defaultValue = "") String career,
                          @RequestParam(value = "resume", required = false) MultipartFile file, Model model) throws IOException {
        Student s = pages.current(me);
        String chosen = CareerCatalog.resolveCareer(career, s.getDepartment());
        model.addAttribute("careers", CareerCatalog.careerNames());
        model.addAttribute("career", chosen);
        Result<String> text = resumes.extractText(file == null || file.isEmpty() ? new byte[0] : file.getBytes());
        if (!text.isOk()) {
            model.addAttribute("formErrors", text.errors());
            return "student/resume";
        }
        model.addAttribute("analysis", resumes.analyze(text.value(), chosen));
        activity.record(s, "Resume analysed", chosen);
        return "student/resume";
    }
}
