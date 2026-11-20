package ai.gradpath.security;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import ai.gradpath.domain.Role;
import ai.gradpath.domain.Student;
import ai.gradpath.domain.User;

/**
 * The signed-in account as seen by the web layer (the Spring Security principal). Built for every request from the
 * revocable server-side session, so role changes and sign-outs take effect immediately.
 */
public record AuthenticatedUser(Integer id, Role role, String name, String email, boolean onboarded, boolean demo) {

    public static AuthenticatedUser of(User user) {
        boolean onboarded = !(user instanceof Student s) || s.isOnboarded();
        return new AuthenticatedUser(user.getId(), user.getRole(), user.getName(), user.getEmail(), onboarded, user.isDemo());
    }

    public boolean isStudent() {
        return role == Role.STUDENT;
    }

    public boolean isOfficer() {
        return role == Role.OFFICER;
    }

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    public String initials() {
        String n = name == null || name.isBlank() ? "?" : name.strip();
        StringBuilder sb = new StringBuilder();
        for (String part : n.split("\\s+")) {
            if (sb.length() < 2 && !part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return sb.toString();
    }

    public String firstName() {
        return name == null || name.isBlank() ? "there" : name.strip().split("\\s+")[0];
    }
}
