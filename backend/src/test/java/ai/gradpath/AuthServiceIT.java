package ai.gradpath;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ai.gradpath.domain.Role;
import ai.gradpath.domain.Student;
import ai.gradpath.domain.User;
import ai.gradpath.service.AuthService;
import ai.gradpath.service.AuthService.Registration;
import ai.gradpath.service.Result;

/** Registration validation, officer access code, sign-in errors and lock-out, legacy hash upgrade, sessions. */
class AuthServiceIT extends IntegrationTestBase {

    @Autowired AuthService auth;

    @Test
    void registersStudentWithBcryptAndNormalisedEmail() {
        String email = uniqueEmail("New.Student").toUpperCase();
        Result<User> r = auth.register(new Registration(Role.STUDENT, "  New   Student ", email, "Passw0rd1", "Passw0rd1", "", ""));
        assertThat(r.isOk()).as(r.error()).isTrue();
        assertThat(r.value()).isInstanceOf(Student.class);
        assertThat(r.value().getEmail()).isEqualTo(email.toLowerCase());
        assertThat(r.value().getName()).isEqualTo("New Student");
        assertThat(r.value().getPasswordHash()).startsWith("$2").doesNotContain("Passw0rd1");
        assertThat(((Student) r.value()).isOnboarded()).isFalse();
    }

    @Test
    void reportsAllValidationErrors() {
        Result<User> r = auth.register(new Registration(Role.STUDENT, "X", "not-an-email", "short", "other", "", ""));
        assertThat(r.isOk()).isFalse();
        assertThat(r.errors()).hasSize(3);
    }

    @Test
    void duplicateEmailIsRejected() {
        String email = uniqueEmail("dup");
        assertThat(auth.register(new Registration(Role.STUDENT, "Dup One", email, "Passw0rd1", "Passw0rd1", "", "")).isOk()).isTrue();
        Result<User> again = auth.register(new Registration(Role.STUDENT, "Dup Two", email.toUpperCase(), "Passw0rd1", "Passw0rd1", "", ""));
        assertThat(again.error()).contains("already exists");
    }

    @Test
    void officerRegistrationRequiresTheAccessCode() {
        Result<User> wrong = auth.register(new Registration(Role.OFFICER, "Olivia Officer", uniqueEmail("o"), "Passw0rd1",
                "Passw0rd1", "Career Office", "WRONG"));
        assertThat(wrong.isOk()).isFalse();
        assertThat(wrong.errors()).anyMatch(e -> e.contains("access code"));
        Result<User> right = auth.register(new Registration(Role.OFFICER, "Olivia Officer", uniqueEmail("o"), "Passw0rd1",
                "Passw0rd1", "Career Office", "TEST-CODE"));
        assertThat(right.isOk()).as(right.error()).isTrue();
        assertThat(right.value().getRole()).isEqualTo(Role.OFFICER);
    }

    @Test
    void signInErrorsAreGenericAndRepeatedFailuresLock() {
        Student s = student("Lock Test");
        Result<User> unknown = auth.authenticate(uniqueEmail("nobody"), "Passw0rd1", "10.0.0.1");
        Result<User> wrongPw = auth.authenticate(s.getEmail(), "WrongPass1", "10.0.0.2");
        assertThat(unknown.error()).isEqualTo(AuthService.GENERIC_LOGIN_ERROR).isEqualTo(wrongPw.error());
        for (int i = 0; i < 4; i++) {
            auth.authenticate(s.getEmail(), "WrongPass1", "10.0.0.2");
        }
        Result<User> locked = auth.authenticate(s.getEmail(), "Passw0rd1", "10.0.0.2");
        assertThat(locked.isOk()).isFalse();
        assertThat(locked.error()).contains("Too many failed sign-in attempts");
    }

    @Test
    void correctPasswordSignsIn() {
        Student s = student("Good Login");
        assertThat(auth.authenticate(s.getEmail().toUpperCase(), "Passw0rd1", "10.0.0.3").isOk()).isTrue();
    }

    @Test
    void legacyPbkdf2HashIsUpgradedToBcryptAtSignIn() throws Exception {
        Student s = student("Legacy User");
        byte[] salt = new byte[16];
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec("OldPass123".toCharArray(), salt, 200_000, 256);
        byte[] digest = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        s.setPasswordHash(java.util.HexFormat.of().formatHex(salt) + "$" + java.util.HexFormat.of().formatHex(digest));
        users.saveAndFlush(s);
        assertThat(auth.authenticate(s.getEmail(), "OldPass123", "10.0.0.4").isOk()).isTrue();
        assertThat(users.findById(s.getId()).orElseThrow().getPasswordHash()).startsWith("$2");
    }

    @Test
    void sessionsResolveAndAreRevocable() {
        Student s = student("Session User");
        String t1 = auth.startSession(s, true);
        String t2 = auth.startSession(s, false);
        assertThat(auth.resolve(t1)).contains(s);
        assertThat(auth.activeSessions(s.getId())).isEqualTo(2);
        auth.logout(t1);
        assertThat(auth.resolve(t1)).isEmpty();
        assertThat(auth.resolve(t2)).isPresent();
        assertThat(auth.logoutEverywhere(s.getId())).isEqualTo(1);
        assertThat(auth.resolve(t2)).isEmpty();
        assertThat(auth.resolve("forged-token")).isEmpty();
    }

    @Test
    void accountWithoutPasswordCannotUseOldSessions() {
        Student s = student("Demo Off");
        String token = auth.startSession(s, true);
        s.setPasswordHash(null);
        users.saveAndFlush(s);
        assertThat(auth.resolve(token)).isEmpty();
    }
}
