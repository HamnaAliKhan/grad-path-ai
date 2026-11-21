package ai.gradpath.service;

import ai.gradpath.domain.CareerOfficer;
import ai.gradpath.domain.Role;
import ai.gradpath.domain.Student;
import ai.gradpath.domain.User;

/** FACTORY PATTERN: callers ask for a role; the factory decides which {@link User} subclass to build. */
public final class UserFactory {

    private UserFactory() { }

    /**
     * Build a new account with normalised identity fields.
     *
     * @param passwordHash an already-hashed password (never a raw password)
     */
    public static User create(Role role, String email, String name, String passwordHash, String office,
                              double defaultCgpaScale) {
        User user = switch (role) {
            case STUDENT -> {
                Student s = new Student();
                s.setCgpaScale(defaultCgpaScale);
                yield s;
            }
            case OFFICER -> {
                CareerOfficer o = new CareerOfficer();
                if (office != null && !office.isBlank()) {
                    o.setOffice(office.strip());
                }
                yield o;
            }
        };
        user.setEmail(email.strip().toLowerCase());
        user.setName(name.strip().replaceAll("\\s+", " "));
        user.setPasswordHash(passwordHash);
        return user;
    }
}
