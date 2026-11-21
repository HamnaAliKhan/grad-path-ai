package ai.gradpath.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;

import ai.gradpath.domain.CareerOfficer;
import ai.gradpath.domain.Student;
import ai.gradpath.domain.User;

/** Account look-ups. */
public interface UserRepository extends BaseRepository<User> {

    /** Find an account by email regardless of letter case. */
    @Query("select u from User u where lower(u.email) = lower(trim(:email))")
    Optional<User> findByEmail(String email);

    @Query("select o from CareerOfficer o order by o.name")
    List<CareerOfficer> officers();

    /** Students who can sign in (seeded demo records without a password are excluded). */
    @Query("select s from Student s where s.passwordHash is not null order by s.name")
    List<Student> registeredStudents();
}
