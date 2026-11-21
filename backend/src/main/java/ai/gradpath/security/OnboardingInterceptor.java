package ai.gradpath.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Students must finish onboarding before using the app; onboarded students are sent away from the onboarding page.
 */
public class OnboardingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser me) || !me.isStudent()) {
            return true;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean onboardingPage = path.equals("/onboarding");
        if (!me.onboarded() && !onboardingPage && !path.equals("/logout")) {
            response.sendRedirect(request.getContextPath() + "/onboarding");
            return false;
        }
        if (me.onboarded() && onboardingPage) {
            response.sendRedirect(request.getContextPath() + "/student");
            return false;
        }
        return true;
    }
}
