package ai.gradpath.domain;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * Shared account data.
 *
 * <p>INHERITANCE: {@link Student} and {@link CareerOfficer} extend this abstract class using JPA joined tables
 * (users + students / career_officers). The users.role column is the discriminator.
 * POLYMORPHISM: {@link #getRole()} and {@link #isDemo()} are answered by each subclass.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 160, nullable = false, unique = true)
    private String email;

    @Column(length = 120, nullable = false)
    private String name;

    /** BCrypt hash (or a legacy PBKDF2 hash that is upgraded at the next sign-in). Null = cannot sign in. */
    @Column(name = "password_hash", length = 300)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = Csv.utcNow();

    /** The account role. */
    public abstract Role getRole();

    /** Whether this is a fictional demonstration account. */
    public abstract boolean isDemo();

    public String displayRole() {
        return getRole().label();
    }

    /** True when the account has a password and can sign in. */
    public boolean canSignIn() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    /** Upper-case initials of the first two name parts, e.g. "Alex Morgan" gives "AM". */
    public String initials() {
        String n = name == null || name.isBlank() ? "?" : name.strip();
        return Arrays.stream(n.split("\\s+")).limit(2)
                .map(p -> p.substring(0, 1)).collect(Collectors.joining()).toUpperCase();
    }

    /** First name for greetings. */
    public String firstName() {
        return name == null || name.isBlank() ? "there" : name.strip().split("\\s+")[0];
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
