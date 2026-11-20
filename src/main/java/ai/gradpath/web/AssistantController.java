package ai.gradpath.web;

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

import ai.gradpath.domain.Student;
import ai.gradpath.security.AuthenticatedUser;
import ai.gradpath.service.assistant.AssistantReply;
import ai.gradpath.service.assistant.CareerAssistantService;
import ai.gradpath.service.assistant.ChatTurn;

/** Career Assistant chat page and its JSON endpoint. */
@Controller
public class AssistantController {

    public static final List<String> SUGGESTIONS = List.of("How can I improve my readiness score?",
            "Which skills should I learn next?", "How do I make my resume stronger?",
            "How do I find an internship?", "How should I prepare for interviews?", "How do I track my goals?");

    private final StudentPages pages;
    private final CareerAssistantService assistant;

    public AssistantController(StudentPages pages, CareerAssistantService assistant) {
        this.pages = pages;
        this.assistant = assistant;
    }

    @GetMapping("/student/assistant")
    public String page(@AuthenticationPrincipal AuthenticatedUser me, Model model) {
        model.addAttribute("assistantLabel", assistant.label());
        model.addAttribute("suggestions", SUGGESTIONS);
        model.addAttribute("firstName", me.firstName());
        return "student/assistant";
    }

    /** Request body: the conversation so far (kept in the browser). */
    public record AskRequest(List<ChatTurn> history) { }

    @PostMapping("/student/assistant/ask")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> ask(@AuthenticationPrincipal AuthenticatedUser me, @RequestBody AskRequest req) {
        if (req == null || req.history() == null || req.history().isEmpty()
                || req.history().get(req.history().size() - 1).content() == null
                || req.history().get(req.history().size() - 1).content().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please type a question."));
        }
        Student s = pages.current(me);
        AssistantReply reply = assistant.ask(s, req.history());
        return ResponseEntity.ok(Map.of("text", reply.text(), "source", reply.source(),
                "notice", reply.notice() == null ? "" : reply.notice()));
    }
}
