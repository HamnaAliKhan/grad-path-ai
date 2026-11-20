package ai.gradpath.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import ai.gradpath.config.GradPathProperties;
import ai.gradpath.readiness.ReadinessRuleEngine;
import ai.gradpath.security.AuthenticatedUser;
import ai.gradpath.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;

/** Model attributes shared by every page: the signed-in user, role-aware navigation and site settings. */
@ControllerAdvice
public class GlobalModelAttributes {

    private final GradPathProperties props;
    private final MessageService messages;

    public GlobalModelAttributes(GradPathProperties props, MessageService messages) {
        this.props = props;
        this.messages = messages;
    }

    @ModelAttribute
    public void common(@AuthenticationPrincipal AuthenticatedUser me, HttpServletRequest request,
                       org.springframework.ui.Model model) {
        // Load the CSRF token before rendering starts so its cookie is written with the response headers.
        Object csrf = request.getAttribute(CsrfToken.class.getName());
        if (csrf instanceof CsrfToken token) {
            token.getToken();
        }
        model.addAttribute("me", me);
        model.addAttribute("appName", GradPathProperties.APP_NAME);
        model.addAttribute("appFullName", GradPathProperties.APP_FULL_NAME);
        model.addAttribute("tagline", GradPathProperties.TAGLINE);
        model.addAttribute("appVersion", GradPathProperties.VERSION);
        model.addAttribute("demoLoginEnabled", props.demoLoginEnabled());
        model.addAttribute("officerSignupEnabled", props.officerSignupEnabled());
        model.addAttribute("disclaimer", ReadinessRuleEngine.DISCLAIMER);
        if (me != null) {
            long unread = messages.unreadCount(me.id());
            model.addAttribute("unreadCount", unread);
            model.addAttribute("nav", nav(me, request.getRequestURI().substring(request.getContextPath().length()), unread));
        }
    }

    static List<NavItem> nav(AuthenticatedUser me, String path, long unread) {
        List<String[]> items = new ArrayList<>();
        if (me.isStudent()) {
            items.add(new String[] {"Overview", "/student", "dashboard"});
            items.add(new String[] {"My Profile", "/student/profile", "person"});
            items.add(new String[] {"Placement Readiness", "/student/readiness", "speed"});
            items.add(new String[] {"Skill-Gap Analysis", "/student/skill-gap", "compare"});
            items.add(new String[] {"Career Explorer", "/student/careers", "explore"});
            items.add(new String[] {"Job Opportunities", "/student/jobs", "work"});
            items.add(new String[] {"Applications", "/student/applications", "assignment"});
            items.add(new String[] {"Resume Analyzer", "/student/resume", "description"});
            items.add(new String[] {"Career Goals", "/student/goals", "flag"});
            items.add(new String[] {"Messages", "/messages", "mail"});
            items.add(new String[] {"Career Assistant", "/student/assistant", "chat"});
        } else {
            items.add(new String[] {"Overview", "/officer", "dashboard"});
            items.add(new String[] {"Students", "/officer/students", "group"});
            items.add(new String[] {"Applications", "/officer/applications", "assignment"});
            items.add(new String[] {"Job Management", "/officer/jobs", "work"});
            items.add(new String[] {"Reports & Analytics", "/officer/reports", "chart"});
            items.add(new String[] {"Messages", "/messages", "mail"});
            items.add(new String[] {"My Profile", "/officer/profile", "person"});
        }
        items.add(new String[] {"How the Score Works", "/rules", "rule"});
        items.add(new String[] {"About the Model", "/model", "model"});
        List<NavItem> out = new ArrayList<>();
        for (String[] i : items) {
            boolean active = i[1].equals("/student") || i[1].equals("/officer") ? path.equals(i[1])
                    : path.equals(i[1]) || path.startsWith(i[1] + "/");
            out.add(new NavItem(i[0], i[1], i[2], i[1].equals("/messages") ? unread : 0, active));
        }
        return out;
    }
}
