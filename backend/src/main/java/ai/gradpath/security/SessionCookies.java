package ai.gradpath.security;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import ai.gradpath.config.GradPathProperties;
import ai.gradpath.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Writes and reads the sign-in cookie: HttpOnly, SameSite=Lax, Secure on HTTPS (or always when configured). */
@Component
public class SessionCookies {

    public static final String NAME = "GP_SESSION";

    private final GradPathProperties props;

    public SessionCookies(GradPathProperties props) {
        this.props = props;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, String token, boolean persistent) {
        ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie.from(NAME, token)
                .httpOnly(true)
                .secure(props.cookieSecure() || request.isSecure())
                .sameSite("Lax")
                .path("/");
        if (persistent) {
            cookie.maxAge(AuthService.PERSISTENT_SESSION);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.build().toString());
        clearCsrf(request, response);
    }

    /** Drop the CSRF token at sign-in / sign-out so a fresh one is issued for the new session. */
    private void clearCsrf(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("XSRF-TOKEN", "").httpOnly(true)
                .secure(props.cookieSecure() || request.isSecure()).sameSite("Lax").path("/").maxAge(Duration.ZERO)
                .build().toString());
    }

    public void clear(HttpServletRequest request, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(NAME, "")
                .httpOnly(true).secure(props.cookieSecure() || request.isSecure()).sameSite("Lax").path("/")
                .maxAge(Duration.ZERO).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        clearCsrf(request, response);
    }

    public String read(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie c : request.getCookies()) {
            if (NAME.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
