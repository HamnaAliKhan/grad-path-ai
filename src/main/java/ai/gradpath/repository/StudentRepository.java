package ai.gradpath.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import ai.gradpath.domain.Student;

/** Student profile queries. */
public interface StudentRepository extends BaseRepository<Student> {

    /** Students by name, optionally excluding fictional demo records. */
    @Query("select s from Student s where (:includeDemo = true or s.demo = false) order by s.name")
    List<Student> listStudents(boolean includeDemo);

    @Query("select s from Student s where s.demo = true order by s.id")
    List<Student> demoStudents();
}
