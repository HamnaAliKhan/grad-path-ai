package ai.gradpath.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ai.gradpath.config.GradPathProperties;
import ai.gradpath.domain.Role;
import ai.gradpath.domain.User;
import ai.gradpath.security.AuthenticatedUser;
import ai.gradpath.security.SessionCookies;
import ai.gradpath.service.AuthService;
import ai.gradpath.service.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Public landing page, sign-in, registration and sign-out. */
@Controller
public class PublicController {

    private final AuthService auth;
    private final SessionCookies cookies;
    private final GradPathProperties props;

    public PublicController(AuthService auth, SessionCookies cookies, GradPathProperties props) {
        this.auth = auth;
        this.cookies = cookies;
        this.props = props;
    }

    @GetMapping("/")
    public String landing(@AuthenticationPrincipal AuthenticatedUser me,
                          @RequestParam(required = false) String signin, Model model) {
        if (me != null) {
            return "redirect:" + home(me);
        }
        if ("required".equals(signin)) {
            model.addAttribute("infoMessage", "Please sign in to continue.");
        }
        model.addAttribute("sampleChart", ChartFactory.landingSample());
        if (!model.containsAttribute("tab")) {
            model.addAttribute("tab", "signin");
        }
        return "public/landing";
    }

    @PostMapping("/login")
    public String login(@RequestParam(defaultValue = "") String email, @RequestParam(defaultValue = "") String password,
                        @RequestParam(defaultValue = "false") boolean remember,
                        HttpServletRequest request, HttpServletResponse response, RedirectAttributes ra) {
        Result<User> result = auth.authenticate(email, password, request.getRemoteAddr());
        if (!result.isOk()) {
            ra.addFlashAttribute("loginErrors", result.errors());
            ra.addFlashAttribute("loginEmail", email.strip());
            ra.addFlashAttribute("tab", "signin");
            return "redirect:/#get-started";
        }
        signIn(result.value(), remember, request, response);
        return "redirect:" + home(AuthenticatedUser.of(result.value()));
    }

    /** Demo sign-in shortcut (only when ENABLE_DEMO_LOGIN=true). */
    @PostMapping("/login/demo")
    public String demo(@RequestParam String account, HttpServletRequest request, HttpServletResponse response,
                       RedirectAttributes ra) {
        if (!props.demoLoginEnabled()) {
            Flash.error(ra, "Demo accounts are disabled on this deployment.");
            return "redirect:/";
        }
        boolean officer = "officer".equals(account);
        Result<User> result = auth.authenticate(
                officer ? GradPathProperties.DEMO_OFFICER_EMAIL : GradPathProperties.DEMO_STUDENT_EMAIL,
                officer ? GradPathProperties.DEMO_OFFICER_PASSWORD : GradPathProperties.DEMO_STUDENT_PASSWORD,
                request.getRemoteAddr());
        if (!result.isOk()) {
            Flash.error(ra, "The demo accounts are not available. Set SEED_DEMO_DATA=true and restart.");
            return "redirect:/";
        }
        signIn(result.value(), false, request, response);
        return "redirect:" + home(AuthenticatedUser.of(result.value()));
    }

    @PostMapping("/register")
    public String register(@RequestParam(defaultValue = "student") String role, @RequestParam(defaultValue = "") String name,
                           @RequestParam(defaultValue = "") String email, @RequestParam(defaultValue = "") String password,
                           @RequestParam(defaultValue = "") String confirm, @RequestParam(defaultValue = "") String office,
                           @RequestParam(defaultValue = "") String accessCode,
                           HttpServletRequest request, HttpServletResponse response, RedirectAttributes ra) {
        Role r = "officer".equals(role) ? Role.OFFICER : Role.STUDENT;
        Result<User> result = auth.register(new AuthService.Registration(r, name, email, password, confirm, office, accessCode));
        if (!result.isOk()) {
            ra.addFlashAttribute("registerErrors", result.errors());
            ra.addFlashAttribute("regName", name.strip());
            ra.addFlashAttribute("regEmail", email.strip());
            ra.addFlashAttribute("regOffice", office.strip());
            ra.addFlashAttribute("regRole", r.code());
            ra.addFlashAttribute("tab", "register");
            return "redirect:/#get-started";
        }
        signIn(result.value(), false, request, response);
        return r == Role.STUDENT ? "redirect:/onboarding" : "redirect:/officer";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, RedirectAttributes ra) {
        auth.logout(cookies.read(request));
        cookies.clear(request, response);
        Flash.success(ra, "You have been signed out.");
        return "redirect:/";
    }

    /** Revoke every session of the account (all browsers and devices). */
    @PostMapping("/logout-all")
    public String logoutAll(@AuthenticationPrincipal AuthenticatedUser me, HttpServletRequest request,
                            HttpServletResponse response, RedirectAttributes ra) {
        int n = auth.logoutEverywhere(me.id());
        cookies.clear(request, response);
        Flash.success(ra, "Signed out of " + n + " session(s) on all devices.");
        return "redirect:/";
    }

    private void signIn(User user, boolean remember, HttpServletRequest request, HttpServletResponse response) {
        String token = auth.startSession(user, remember);
        cookies.write(request, response, token, remember);
    }

    static String home(AuthenticatedUser me) {
        if (me.isOfficer()) {
            return "/officer";
        }
        return me.onboarded() ? "/student" : "/onboarding";
    }
}
