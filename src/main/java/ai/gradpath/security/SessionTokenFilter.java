package ai.gradpath.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import ai.gradpath.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates each request from the GP_SESSION cookie by looking up the revocable server-side session.
 * Invalid or revoked tokens are cleared from the browser.
 */
public class SessionTokenFilter extends OncePerRequestFilter {

    private final AuthService auth;
    private final SessionCookies cookies;

    public SessionTokenFilter(AuthService auth, SessionCookies cookies) {
        this.auth = auth;
        this.cookies = cookies;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/img/")
                || path.equals("/favicon.svg") || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = cookies.read(request);
        if (token != null) {
            var user = auth.resolve(token);
            if (user.isPresent()) {
                AuthenticatedUser principal = AuthenticatedUser.of(user.get());
                SecurityContext ctx = SecurityContextHolder.createEmptyContext();
                ctx.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null,
                        principal.authorities()));
                SecurityContextHolder.setContext(ctx);
            } else {
                cookies.clear(request, response);
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
