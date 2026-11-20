package ai.gradpath.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Password hashing with BCrypt (salted, deliberately slow; Spring Security's implementation).
 *
 * <p>Accounts created by the earlier version store salted PBKDF2-HMAC-SHA256 hashes ({@code saltHex$digestHex}).
 * Those are still verified, and {@link #needsUpgrade} tells the caller to re-hash them with BCrypt after the next
 * successful sign-in, so existing users never have to reset their password.
 */
@Component
public class PasswordService {

    private static final int LEGACY_ITERATIONS = 200_000;
    private static final HexFormat HEX = HexFormat.of();

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);

    public String hash(String rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    /** Constant-time verification of BCrypt or legacy PBKDF2 hashes. */
    public boolean matches(String rawPassword, String stored) {
        if (rawPassword == null || stored == null || stored.isBlank()) {
            return false;
        }
        if (isBcrypt(stored)) {
            return bcrypt.matches(rawPassword, stored);
        }
        return legacyMatches(rawPassword, stored);
    }

    /** True for hashes that should be replaced by a BCrypt hash. */
    public boolean needsUpgrade(String stored) {
        return stored != null && !isBcrypt(stored);
    }

    private static boolean isBcrypt(String stored) {
        return stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$");
    }

    private static boolean legacyMatches(String password, String stored) {
        int sep = stored.indexOf('$');
        if (sep <= 0) {
            return false;
        }
        try {
            byte[] salt = HEX.parseHex(stored.substring(0, sep));
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, LEGACY_ITERATIONS, 256);
            byte[] digest = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(HEX.formatHex(digest).getBytes(StandardCharsets.US_ASCII),
                    stored.substring(sep + 1).getBytes(StandardCharsets.US_ASCII));
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            return false;
        }
    }

    /** Hex SHA-256 of a session token (only hashes of tokens are stored). */
    public static String sha256Hex(String value) {
        try {
            return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
