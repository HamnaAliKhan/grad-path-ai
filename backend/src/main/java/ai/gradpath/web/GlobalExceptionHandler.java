package ai.gradpath.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ai.gradpath.readiness.InvalidProfileException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Turns expected failures into helpful, recoverable pages. Messages never include stack traces, SQL, connection
 * strings or other internal details.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** A stored profile value is outside the documented rule ranges. */
    @ExceptionHandler(InvalidProfileException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public String invalidProfile(InvalidProfileException e, Model model) {
        model.addAttribute("title", "Your profile needs attention");
        model.addAttribute("message", e.getMessage() + " Please update your profile so the readiness rules can be applied.");
        model.addAttribute("actionHref", "/student/profile/edit");
        model.addAttribute("actionLabel", "Update profile");
        return "error/problem";
    }

    /** Upload larger than the configured limit. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge(HttpServletRequest request, RedirectAttributes ra) {
        Flash.error(ra, "The file is too large. Please upload a PDF of at most 5 MB.");
        String path = request.getRequestURI();
        return "redirect:" + (path.startsWith("/student/jobs/") ? path.replace("/apply", "") : "/student/resume");
    }

    /** Database unreachable (e.g. Neon waking up) - recoverable. */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String database(DataAccessException e, Model model) {
        log.warn("Database error: {}", e.getClass().getSimpleName());
        model.addAttribute("title", "The database is briefly unavailable");
        model.addAttribute("message", "The database may be waking up or busy. Your data is safe - please try again in a few seconds.");
        model.addAttribute("actionHref", "");
        model.addAttribute("actionLabel", "Try again");
        return "error/problem";
    }
}
