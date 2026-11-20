package ai.gradpath.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.springframework.stereotype.Component;

/**
 * Brute-force protection for sign-in: 5 failures for one email lock that email for 5 minutes, and
 * 20 failures from one client address lock the address for 15 minutes. Counters live in memory.
 */
@Component
public class LoginThrottle {

    public static final int MAX_EMAIL_FAILS = 5;
    public static final long EMAIL_LOCK_MILLIS = 5 * 60_000L;
    public static final int MAX_IP_FAILS = 20;
    public static final long IP_LOCK_MILLIS = 15 * 60_000L;

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public LoginThrottle() {
        this(System::currentTimeMillis);
    }

    public LoginThrottle(LongSupplier clock) {
        this.clock = clock;
    }

    private static final class Counter {
        int fails;
        long lockedUntil;
        long firstFail;
    }

    /** Seconds until sign-in is allowed again for this email/address pair (0 = allowed). */
    public long secondsLocked(String email, String ip) {
        long now = clock.getAsLong();
        long until = Math.max(until("e:" + email), until("i:" + ip));
        return until > now ? (until - now) / 1000 + 1 : 0;
    }

    public void recordFailure(String email, String ip) {
        bump("e:" + email, MAX_EMAIL_FAILS, EMAIL_LOCK_MILLIS);
        bump("i:" + ip, MAX_IP_FAILS, IP_LOCK_MILLIS);
    }

    public void recordSuccess(String email) {
        counters.remove("e:" + email);
    }

    private long until(String key) {
        Counter c = counters.get(key);
        return c == null ? 0 : c.lockedUntil;
    }

    private void bump(String key, int max, long lockMillis) {
        Counter c = counters.computeIfAbsent(key, k -> new Counter());
        synchronized (c) {
            long now = clock.getAsLong();
            if (now - c.firstFail > lockMillis) {
                c.fails = 0;
                c.firstFail = now;
            }
            c.fails++;
            if (c.fails >= max) {
                c.fails = 0;
                c.lockedUntil = now + lockMillis;
            }
        }
    }
}
