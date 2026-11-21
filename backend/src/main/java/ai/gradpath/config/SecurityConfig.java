package ai.gradpath.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import ai.gradpath.security.SessionCookies;
import ai.gradpath.security.SessionTokenFilter;
import ai.gradpath.service.AuthService;

/**
 * Web security: role-based route authorisation, CSRF protection for every state-changing request, security headers,
 * and authentication from revocable server-side sessions ({@link SessionTokenFilter}).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self' https://cdnjs.cloudflare.com",
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
            "font-src 'self' https://fonts.gstatic.com data:",
            "img-src 'self' data: blob:",
            "connect-src 'self'",
            "frame-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'self'");

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthService auth, SessionCookies cookies) throws Exception {
        CookieCsrfTokenRepository csrfRepo = new CookieCsrfTokenRepository();
        csrfRepo.setCookieCustomizer(c -> c.sameSite("Lax").httpOnly(true));
        http
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Sessions are authenticated per request from the GP_SESSION cookie, so the default "rotate the CSRF token on
            // authentication" strategy would rotate it on EVERY request and break pages that post more than once.
            .csrf(c -> c.csrfTokenRepository(csrfRepo)
                    .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .addFilterBefore(new SessionTokenFilter(auth, cookies), AnonymousAuthenticationFilter.class)
            .authorizeHttpRequests(a -> a
                .requestMatchers("/", "/login", "/login/demo", "/register", "/logout", "/css/**", "/js/**", "/img/**", "/favicon.svg",
                        "/error", "/error/**").permitAll()
                .requestMatchers("/onboarding", "/student/**").hasRole("STUDENT")
                .requestMatchers("/officer/**").hasRole("OFFICER")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> {
                    if (wantsJson(req.getHeader("Accept"))) {
                        res.sendError(HttpStatus.UNAUTHORIZED.value());
                    } else {
                        res.sendRedirect(req.getContextPath() + "/?signin=required");
                    }
                })
                .accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(HttpStatus.FORBIDDEN.value());
                    req.getRequestDispatcher("/error/403").forward(req, res);
                }))
            .headers(h -> h
                .contentSecurityPolicy(c -> c.policyDirectives(CSP))
                .frameOptions(f -> f.sameOrigin())
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=()")));
        return http.build();
    }

    private static boolean wantsJson(String accept) {
        return accept != null && accept.contains("application/json") && !accept.contains("text/html");
    }
}
