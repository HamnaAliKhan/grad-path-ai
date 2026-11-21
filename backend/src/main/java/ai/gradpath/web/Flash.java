package ai.gradpath.web;

import java.util.List;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Helpers for one-time success / error messages shown after a redirect. */
public final class Flash {

    private Flash() { }

    public static void success(RedirectAttributes ra, String message) {
        ra.addFlashAttribute("flashSuccess", message);
    }

    public static void error(RedirectAttributes ra, String message) {
        ra.addFlashAttribute("flashErrors", List.of(message));
    }

    public static void errors(RedirectAttributes ra, List<String> messages) {
        ra.addFlashAttribute("flashErrors", messages);
    }
}
