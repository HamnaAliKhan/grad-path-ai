package ai.gradpath.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import ai.gradpath.domain.Student;
import ai.gradpath.reference.Career;
import ai.gradpath.reference.CareerCatalog;
import ai.gradpath.security.AuthenticatedUser;
import ai.gradpath.service.NotFoundException;
import ai.gradpath.service.SkillGapService;

/** Skill-Gap Analysis and Career Explorer. */
@Controller
public class CareerController {

    /** A career and the student's current skill match with it. */
    public record CareerCard(Career career, int match) { }

    private final StudentPages pages;
    private final SkillGapService skillGap;

    public CareerController(StudentPages pages, SkillGapService skillGap) {
        this.pages = pages;
        this.skillGap = skillGap;
    }

    @GetMapping("/student/skill-gap")
    public String skillGap(@AuthenticationPrincipal AuthenticatedUser me,
                           @RequestParam(required = false) String career, Model model) {
        Student s = pages.current(me);
        String chosen = CareerCatalog.resolveCareer(career != null ? career : s.getTargetCareer(), s.getDepartment());
        model.addAttribute("careers", CareerCatalog.careerNames());
        model.addAttribute("career", chosen);
        model.addAttribute("gap", skillGap.analyze(s, chosen));
        model.addAttribute("catalogDisclaimer", CareerCatalog.DISCLAIMER);
        return "student/skill-gap";
    }

    @GetMapping("/student/careers")
    public String explorer(@AuthenticationPrincipal AuthenticatedUser me, Model model) {
        Student s = pages.current(me);
        model.addAttribute("careers", CareerCatalog.CAREERS.values().stream()
                .map(c -> new CareerCard(c, skillGap.analyze(s, c.name()).match())).toList());
        model.addAttribute("target", s.getTargetCareer());
        model.addAttribute("catalogDisclaimer", CareerCatalog.DISCLAIMER);
        return "student/careers";
    }

    @GetMapping("/student/careers/{slug}")
    public String career(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String slug, Model model) {
        Student s = pages.current(me);
        Career c = CareerCatalog.bySlug(slug).orElseThrow(() -> new NotFoundException("Career not found."));
        model.addAttribute("career", c);
        model.addAttribute("gap", skillGap.analyze(s, c.name()));
        model.addAttribute("catalogDisclaimer", CareerCatalog.DISCLAIMER);
        return "student/career-detail";
    }
}
