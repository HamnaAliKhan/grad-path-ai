package ai.gradpath.web;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ai.gradpath.domain.Message;
import ai.gradpath.domain.User;
import ai.gradpath.repository.UserRepository;
import ai.gradpath.security.AuthenticatedUser;
import ai.gradpath.service.MessageService;
import ai.gradpath.service.NotFoundException;
import ai.gradpath.service.Result;

/**
 * In-app messaging for students and officers. A thread is always "me + partner", so nobody can read other people's
 * conversations; recipients are validated by {@link MessageService#canMessage}.
 */
@Controller
public class MessageController {

    private final MessageService messages;
    private final UserRepository users;

    public MessageController(MessageService messages, UserRepository users) {
        this.messages = messages;
        this.users = users;
    }

    @GetMapping("/messages")
    public String inbox(@AuthenticationPrincipal AuthenticatedUser me, Model model) {
        User user = self(me);
        List<MessageService.Conversation> convs = messages.conversations(me.id());
        model.addAttribute("conversations", convs);
        model.addAttribute("contacts", messages.contacts(user));
        return "messages/index";
    }

    @GetMapping("/messages/{partnerId}")
    public String thread(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Integer partnerId, Model model) {
        User user = self(me);
        User partner = partnerFor(user, partnerId);
        model.addAttribute("conversations", messages.conversations(me.id()));
        model.addAttribute("contacts", messages.contacts(user));
        model.addAttribute("partner", partner);
        model.addAttribute("thread", messages.thread(me.id(), partnerId));
        model.addAttribute("canReply", messages.canMessage(user, partner));
        return "messages/index";
    }

    /** Thread fragment used by the auto-refresh script. */
    @GetMapping("/messages/{partnerId}/thread")
    public String threadFragment(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Integer partnerId, Model model) {
        User user = self(me);
        partnerFor(user, partnerId);
        model.addAttribute("thread", messages.thread(me.id(), partnerId));
        return "messages/index :: thread";
    }

    @PostMapping("/messages/{partnerId}")
    public String send(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Integer partnerId,
                       @RequestParam(defaultValue = "") String body, RedirectAttributes ra) {
        Result<Message> r = messages.send(self(me), partnerId, body);
        if (!r.isOk()) {
            Flash.errors(ra, r.errors());
            ra.addFlashAttribute("draft", body);
        }
        return "redirect:/messages/" + partnerId + "#composer";
    }

    @PostMapping("/messages/start")
    public String start(@RequestParam(required = false) Integer recipient, RedirectAttributes ra) {
        if (recipient == null) {
            Flash.error(ra, "Choose a person to message.");
            return "redirect:/messages";
        }
        return "redirect:/messages/" + recipient;
    }

    private User self(AuthenticatedUser me) {
        return users.findById(me.id()).orElseThrow(() -> new NotFoundException("Account not found."));
    }

    /** The partner when a conversation between the two accounts is allowed or already exists. */
    private User partnerFor(User me, Integer partnerId) {
        Optional<User> partner = messages.partner(partnerId);
        boolean hasHistory = partner.isPresent() && messages.conversations(me.getId()).stream()
                .anyMatch(c -> c.partnerId().equals(partnerId));
        if (partner.isEmpty() || !(hasHistory || messages.canMessage(me, partner.get()))) {
            throw new NotFoundException("Conversation not found.");
        }
        return partner.get();
    }
}
