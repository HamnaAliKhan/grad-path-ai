package ai.gradpath.web;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ai.gradpath.domain.CareerGoal;
import ai.gradpath.domain.GoalTask;
import ai.gradpath.security.AuthenticatedUser;
import ai.gradpath.service.GoalService;
import ai.gradpath.service.Result;

/** Career goals and tasks (every change is checked for ownership in GoalService). */
@Controller
public class GoalController {

    private final GoalService goals;

    public GoalController(GoalService goals) {
        this.goals = goals;
    }

    @GetMapping("/student/goals")
    public String page(@AuthenticationPrincipal AuthenticatedUser me, @RequestParam(defaultValue = "goals") String tab,
                       @RequestParam(required = false) Integer edit, Model model) {
        List<CareerGoal> list = goals.list(me.id());
        model.addAttribute("goals", list);
        model.addAttribute("summary", goals.summary(list));
        model.addAttribute("tab", List.of("goals", "pending", "done").contains(tab) ? tab : "goals");
        model.addAttribute("editId", edit);
        return "student/goals";
    }

    @PostMapping("/student/goals")
    public String create(@AuthenticationPrincipal AuthenticatedUser me, @RequestParam(defaultValue = "") String title,
                         @RequestParam(defaultValue = "") String description, RedirectAttributes ra) {
        Result<CareerGoal> r = goals.create(me.id(), title, description);
        if (r.isOk()) {
            Flash.success(ra, "Goal created. Add small tasks to track your progress.");
        } else {
            Flash.errors(ra, r.errors());
            ra.addFlashAttribute("newTitle", title);
            ra.addFlashAttribute("newDescription", description);
        }
        return "redirect:/student/goals";
    }

    @PostMapping("/student/goals/{id}")
    public String update(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Integer id,
                         @RequestParam(defaultValue = "") String title, @RequestParam(defaultValue = "") String description,
                         RedirectAttributes ra) {
        Result<CareerGoal> r = goals.update(me.id(), id, title, description);
        if (r.isOk()) {
            Flash.success(ra, "Goal updated.");
            return "redirect:/student/goals#goal-" + id;
        }
        Flash.errors(ra, r.errors());
        return "redirect:/student/goals?edit=" + id + "#goal-" + id;
    }

    @PostMapping("/student/goals/{id}/delete")
    public String delete(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Integer id, RedirectAttributes ra) {
        String title = goals.delete(me.id(), id);
        Flash.success(ra, "Deleted the goal \"" + title + "\" and its tasks.");
        return "redirect:/student/goals";
    }

    @PostMapping("/student/goals/{id}/tasks")
    public String addTask(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Integer id,
                          @RequestParam(defaultValue = "") String title, RedirectAttributes ra) {
        Result<GoalTask> r = goals.addTask(me.id(), id, title);
        if (!r.isOk()) {
            Flash.errors(ra, r.errors());
        }
        return "redirect:/student/goals#goal-" + id;
    }

    @PostMapping("/student/tasks/{id}/done")
    public String setDone(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Integer id,
                          @RequestParam(defaultValue = "false") boolean done, @RequestParam(defaultValue = "goals") String tab) {
        GoalTask t = goals.setDone(me.id(), id, done);
        return "redirect:/student/goals?tab=" + tab + "#goal-" + t.getGoal().getId();
    }

    @PostMapping("/student/tasks/{id}/delete")
    public String deleteTask(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Integer id, RedirectAttributes ra) {
        goals.deleteTask(me.id(), id);
        Flash.success(ra, "Task removed.");
        return "redirect:/student/goals";
    }
}
