package ai.gradpath.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import ai.gradpath.domain.ReadinessAssessment;
import ai.gradpath.domain.Student;
import ai.gradpath.readiness.InvalidProfileException;
import ai.gradpath.readiness.ProfileSnapshot;
import ai.gradpath.readiness.ReadinessCategory;
import ai.gradpath.readiness.ReadinessFormat;
import ai.gradpath.readiness.ReadinessResult;
import ai.gradpath.security.AuthenticatedUser;
import ai.gradpath.service.PlacementModelService;
import ai.gradpath.service.ReadinessService;
import ai.gradpath.service.RecommendationService;

/** Placement Readiness page and the what-if simulator (what-if results are never saved). */
@Controller
public class ReadinessController {

    private final StudentPages pages;
    private final ReadinessService readiness;
    private final RecommendationService recommendations;
    private final PlacementModelService placementModel;

    public ReadinessController(StudentPages pages, ReadinessService readiness, RecommendationService recommendations,
                               PlacementModelService placementModel) {
        this.placementModel = placementModel;
        this.pages = pages;
        this.readiness = readiness;
        this.recommendations = recommendations;
    }

    @GetMapping("/student/readiness")
    public String page(@AuthenticationPrincipal AuthenticatedUser me, Model model) {
        Student s = pages.current(me);
        ReadinessResult r = readiness.record(s);           // saved to history only when it changed
        List<ReadinessAssessment> history = readiness.history(s.getId());
        model.addAttribute("student", s);
        model.addAttribute("result", r);
        model.addAttribute("categories", ReadinessCategory.values());
        model.addAttribute("plan", recommendations.levers(s, 5));
        model.addAttribute("actions", recommendations.nextSteps(r, 7));
        model.addAttribute("history", history);
        model.addAttribute("legacyCount", readiness.legacyCount(s.getId()));
        model.addAttribute("gaugeChart", ChartFactory.gauge(r.score(), r.category()));
        model.addAttribute("factorChart", ChartFactory.factorBars(r.factors()));
        model.addAttribute("historyChart", history.size() >= 2 ? ChartFactory.history(history) : null);
        var estimate = placementModel.estimate(s).orElse(null);
        model.addAttribute("estimate", estimate);
        model.addAttribute("modelDisclaimer", PlacementModelService.DISCLAIMER);
        model.addAttribute("contributionChart", estimate == null ? null : ChartFactory.contributions(estimate.contributions()));
        return "student/readiness";
    }

    /** What-if values posted by the simulator. */
    public record WhatIf(Double cgpa, Integer projects, Integer internships, Integer workMonths, Integer confidentSkills,
                         Integer learningSkills, Integer certifications, Integer communication, Integer extracurricular) { }

    @PostMapping("/student/readiness/what-if")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> whatIf(@AuthenticationPrincipal AuthenticatedUser me, @RequestBody WhatIf w) {
        Student s = pages.current(me);
        ProfileSnapshot base = ProfileSnapshot.of(s);
        try {
            if (w.cgpa() == null || w.projects() == null || w.internships() == null || w.workMonths() == null
                    || w.confidentSkills() == null || w.learningSkills() == null || w.certifications() == null
                    || w.communication() == null || w.extracurricular() == null) {
                throw new InvalidProfileException("all", "Please provide every what-if value.");
            }
            ProfileSnapshot changed = new ProfileSnapshot(w.cgpa(), base.cgpaScale(), w.projects(), w.internships(),
                    w.workMonths(), w.confidentSkills(), w.learningSkills(), w.certifications(), w.communication(),
                    w.extracurricular(), base.profileCompletion(), base.missingInformation());
            ReadinessResult now = readiness.evaluate(base);
            ReadinessResult next = readiness.evaluate(changed);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("score", next.score());
            out.put("scoreLabel", next.scoreLabel());
            out.put("category", next.category().label());
            out.put("badge", next.category().badge());
            out.put("delta", ReadinessFormat.round1(next.score() - now.score()));
            out.put("calculation", next.calculation());
            placementModel.estimate(changed).ifPresent(e -> {
                out.put("modelPercent", e.percentLabel());
                out.put("modelBand", e.band());
                out.put("modelBadge", e.badge());
                out.put("modelDelta", e.percent() - placementModel.estimate(base).orElseThrow().percent());
            });
            out.put("factors", next.factors().stream().map(f -> Map.of("name", f.name(), "points", f.points(),
                    "max", f.maxPoints(), "rules", f.rulesLabel())).toList());
            return ResponseEntity.ok(out);
        } catch (InvalidProfileException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
