package ai.gradpath.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import ai.gradpath.domain.AuthSession;

/** Revocable sign-in sessions. */
public interface AuthSessionRepository extends BaseRepository<AuthSession> {

    Optional<AuthSession> findByTokenHash(String tokenHash);

    long countByUserId(Integer userId);

    /** Revoke every session of an account ("sign out everywhere"). */
    @Modifying
    @Query("delete from AuthSession a where a.userId = :userId")
    int deleteForUser(Integer userId);

    @Modifying
    @Query("delete from AuthSession a where a.expiresAt < :now")
    int deleteExpired(LocalDateTime now);
}
