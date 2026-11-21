package ai.gradpath.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import ai.gradpath.readiness.FactorScorer;
import ai.gradpath.readiness.ReadinessCategory;
import ai.gradpath.readiness.ReadinessRuleEngine;
import ai.gradpath.readiness.RuleDefinition;
import ai.gradpath.service.FitCalculator;
import jakarta.servlet.http.HttpServletResponse;

/** "How the score works": every rule, the weights, the categories and the job-fit formula. Plus friendly 403 page. */
@Controller
public class RulesController {

    private final ReadinessRuleEngine engine;

    public RulesController(ReadinessRuleEngine engine) {
        this.engine = engine;
    }

    @GetMapping("/rules")
    public String rules(Model model) {
        Map<String, List<RuleDefinition>> grouped = new LinkedHashMap<>();
        engine.rules().forEach(r -> grouped.computeIfAbsent(r.factor(), k -> new java.util.ArrayList<>()).add(r));
        model.addAttribute("grouped", grouped);
        model.addAttribute("ruleCount", engine.rules().size());
        model.addAttribute("weights", engine.scorers().stream().map(s -> Map.entry(s.name(), s.maxPoints())).toList());
        model.addAttribute("totalWeight", engine.scorers().stream().mapToDouble(FactorScorer::maxPoints).sum());
        model.addAttribute("categories", ReadinessCategory.values());
        model.addAttribute("version", ReadinessRuleEngine.VERSION);
        model.addAttribute("fitWeights", List.of(FitCalculator.W_SKILL, FitCalculator.W_RESUME, FitCalculator.W_READINESS));
        return "rules";
    }

    @RequestMapping("/error/403")
    public String forbidden(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return "error/403";
    }
}
