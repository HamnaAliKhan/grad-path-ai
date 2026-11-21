package ai.gradpath.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.gradpath.config.GradPathProperties;
import ai.gradpath.domain.AuthSession;
import ai.gradpath.domain.Csv;
import ai.gradpath.domain.Role;
import ai.gradpath.domain.User;
import ai.gradpath.repository.AuthSessionRepository;
import ai.gradpath.repository.UserRepository;
import ai.gradpath.security.LoginThrottle;
import ai.gradpath.security.PasswordService;
import ai.gradpath.validation.Validators;

/**
 * Registration, sign-in and revocable sign-in sessions.
 *
 * <ul>
 *   <li>Passwords are BCrypt hashes; legacy PBKDF2 hashes are upgraded after a successful sign-in.</li>
 *   <li>Sign-in errors are generic (no account enumeration) and repeated failures are throttled.</li>
 *   <li>Officer registration requires OFFICER_ACCESS_CODE and is disabled when no code is configured.</li>
 *   <li>A session is a random 256-bit token in an HttpOnly cookie; only its SHA-256 is stored.
 *       "Keep me signed in" sessions last 30 days, others 12 hours. Deleting the row revokes the session.</li>
 * </ul>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public static final Duration PERSISTENT_SESSION = Duration.ofDays(30);
    public static final Duration BROWSER_SESSION = Duration.ofHours(12);
    public static final String GENERIC_LOGIN_ERROR =
            "We couldn't sign you in. Check your email and password, or create an account.";
    private static final Duration TOUCH_INTERVAL = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Registration form values. */
    public record Registration(Role role, String name, String email, String password, String confirm,
                               String office, String accessCode) { }

    private final UserRepository users;
    private final AuthSessionRepository sessions;
    private final PasswordService passwords;
    private final LoginThrottle throttle;
    private final GradPathProperties props;
    private final ActivityService activity;
    /** Verified against unknown emails so failed look-ups take about as long as wrong passwords. */
    private final String dummyHash;

    public AuthService(UserRepository users, AuthSessionRepository sessions, PasswordService passwords,
                       LoginThrottle throttle, GradPathProperties props, ActivityService activity) {
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
        this.throttle = throttle;
        this.props = props;
        this.activity = activity;
        this.dummyHash = passwords.hash("not-a-real-password-1");
    }

    /** Validate registration data and create the account. */
    @Transactional
    public Result<User> register(Registration r) {
        List<String> errors = new ArrayList<>();
        if (r.role() == null) {
            return Result.fail("Choose an account type.");
        }
        addIf(errors, Validators.checkName(r.name()));
        addIf(errors, Validators.checkEmail(r.email()));
        addIf(errors, Validators.checkPassword(r.password(), r.confirm()));
        if (r.role() == Role.OFFICER) {
            if (!props.officerSignupEnabled()) {
                return Result.fail("Career Officer registration is switched off. The site owner must configure an officer access code.");
            }
            addIf(errors, Validators.checkText(r.office(), "Office / organisation name", 3, 120));
            String given = r.accessCode() == null ? "" : r.accessCode().strip();
            if (!MessageDigest.isEqual(given.getBytes(StandardCharsets.UTF_8),
                    props.officerAccessCode().strip().getBytes(StandardCharsets.UTF_8))) {
                errors.add("The officer access code is not correct. Ask your university's Career Office for it.");
            }
        }
        if (!errors.isEmpty()) {
            return Result.fail(errors);
        }
        String email = Validators.normalizeEmail(r.email());
        if (users.findByEmail(email).isPresent()) {
            return Result.fail("An account with this email already exists. Try signing in instead.");
        }
        User user = UserFactory.create(r.role(), email, Validators.clean(r.name()), passwords.hash(r.password()),
                r.office(), props.safeDefaultScale());
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException race) {
            return Result.fail("An account with this email already exists. Try signing in instead.");
        }
        activity.record(user, "Account created", user.displayRole());
        return Result.ok(user);
    }

    /** Check credentials. Errors are generic; repeated failures lock the email/address temporarily. */
    @Transactional
    public Result<User> authenticate(String email, String password, String clientIp) {
        String key = Validators.normalizeEmail(email);
        String ip = clientIp == null ? "?" : clientIp;
        long wait = throttle.secondsLocked(key, ip);
        if (wait > 0) {
            return Result.fail("Too many failed sign-in attempts. Please try again in " + Math.max(1, (wait + 59) / 60)
                    + " minute(s).");
        }
        Optional<User> user = key.isEmpty() ? Optional.empty() : users.findByEmail(key);
        String stored = user.map(User::getPasswordHash).orElse(null);
        boolean ok = passwords.matches(password, stored != null ? stored : dummyHash) && stored != null;
        if (!ok) {
            throttle.recordFailure(key, ip);
            return Result.fail(GENERIC_LOGIN_ERROR);
        }
        throttle.recordSuccess(key);
        User u = user.get();
        if (passwords.needsUpgrade(stored)) {
            u.setPasswordHash(passwords.hash(password));     // legacy PBKDF2 -> BCrypt
        }
        return Result.ok(u);
    }

    /** Create a server-side session and return the raw token for the cookie. */
    @Transactional
    public String startSession(User user, boolean persistent) {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        Duration life = persistent ? PERSISTENT_SESSION : BROWSER_SESSION;
        sessions.save(new AuthSession(user.getId(), PasswordService.sha256Hex(token), Csv.utcNow().plus(life), persistent));
        return token;
    }

    /** The account behind a session token, if the session exists and has not expired. */
    @Transactional
    public Optional<User> resolve(String token) {
        if (token == null || token.isBlank() || token.length() > 100) {
            return Optional.empty();
        }
        Optional<AuthSession> found = sessions.findByTokenHash(PasswordService.sha256Hex(token));
        LocalDateTime now = Csv.utcNow();
        if (found.isEmpty()) {
            return Optional.empty();
        }
        AuthSession session = found.get();
        if (session.isExpired(now)) {
            sessions.delete(session);
            return Optional.empty();
        }
        Optional<User> user = users.findById(session.getUserId());
        if (user.isEmpty() || !user.get().canSignIn()) {
            sessions.delete(session);       // account removed or demo login switched off
            return Optional.empty();
        }
        if (session.getLastUsedAt() == null || session.getLastUsedAt().plus(TOUCH_INTERVAL).isBefore(now)) {
            session.setLastUsedAt(now);
        }
        return user;
    }

    /** Revoke one session. */
    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            sessions.findByTokenHash(PasswordService.sha256Hex(token)).ifPresent(sessions::delete);
        }
    }

    /** Revoke every session of an account. */
    @Transactional
    public int logoutEverywhere(Integer userId) {
        return sessions.deleteForUser(userId);
    }

    @Transactional(readOnly = true)
    public long activeSessions(Integer userId) {
        return sessions.countByUserId(userId);
    }

    /**
     * Demo accounts have public passwords: give them their passwords only when demo login is enabled, and remove
     * the passwords (and revoke their sessions) when it is disabled.
     */
    @Transactional
    public void syncDemoAccounts(boolean enabled) {
        String[][] demo = {{GradPathProperties.DEMO_STUDENT_EMAIL, GradPathProperties.DEMO_STUDENT_PASSWORD},
                           {GradPathProperties.DEMO_OFFICER_EMAIL, GradPathProperties.DEMO_OFFICER_PASSWORD}};
        for (String[] account : demo) {
            users.findByEmail(account[0]).ifPresent(user -> {
                if (enabled && !user.canSignIn()) {
                    user.setPasswordHash(passwords.hash(account[1]));
                } else if (!enabled) {
                    user.setPasswordHash(null);
                    sessions.deleteForUser(user.getId());
                }
            });
        }
    }

    /** Daily clean-up of expired sessions. */
    @Scheduled(fixedDelay = 6 * 60 * 60 * 1000L, initialDelay = 60_000L)
    @Transactional
    public void purgeExpiredSessions() {
        int n = sessions.deleteExpired(Csv.utcNow());
        if (n > 0) {
            log.info("Removed {} expired sign-in session(s)", n);
        }
    }

    private static void addIf(List<String> errors, String error) {
        if (error != null) {
            errors.add(error);
        }
    }
}
