package ai.gradpath.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import ai.gradpath.domain.CareerGoal;

/** Career goals (tasks are reached through their goal). */
public interface GoalRepository extends BaseRepository<CareerGoal> {

    @Query("select g from CareerGoal g where g.student.id = :studentId order by g.id")
    List<CareerGoal> forStudent(Integer studentId);
}
