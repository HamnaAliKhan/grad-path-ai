package ai.gradpath.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.gradpath.domain.CareerGoal;
import ai.gradpath.domain.GoalTask;
import ai.gradpath.domain.Student;
import ai.gradpath.repository.GoalRepository;
import ai.gradpath.repository.GoalTaskRepository;
import ai.gradpath.repository.StudentRepository;

/**
 * Career goals and tasks. Every change verifies that the goal or task belongs to the signed-in student;
 * someone else's goal is reported as "not found".
 */
@Service
@PreAuthorize("hasRole('STUDENT')")
public class GoalService {

    private final GoalRepository goals;
    private final GoalTaskRepository tasks;
    private final StudentRepository students;
    private final ActivityService activity;

    public GoalService(GoalRepository goals, GoalTaskRepository tasks, StudentRepository students,
                       ActivityService activity) {
        this.goals = goals;
        this.tasks = tasks;
        this.students = students;
        this.activity = activity;
    }

    /** Totals across all goals and the next pending task. */
    public record Summary(int goals, int total, int done, int pending, int progress, String next) { }

    @Transactional(readOnly = true)
    public List<CareerGoal> list(Integer studentId) {
        List<CareerGoal> list = goals.forStudent(studentId);
        list.forEach(g -> g.getTasks().size());
        return list;
    }

    @Transactional
    public Result<CareerGoal> create(Integer studentId, String title, String description) {
        String error = checkGoal(title, description);
        if (error != null) {
            return Result.fail(error);
        }
        Student student = students.findById(studentId).orElseThrow(() -> new NotFoundException("Student not found."));
        CareerGoal goal = new CareerGoal(title.strip(), description == null ? "" : description.strip());
        student.addGoal(goal);
        goals.save(goal);
        activity.record(student, "Goal created", goal.getTitle());
        return Result.ok(goal);
    }

    @Transactional
    public Result<CareerGoal> update(Integer studentId, Integer goalId, String title, String description) {
        CareerGoal goal = ownedGoal(studentId, goalId);
        String error = checkGoal(title, description);
        if (error != null) {
            return Result.fail(error);
        }
        goal.setTitle(title.strip());
        goal.setDescription(description == null ? "" : description.strip());
        return Result.ok(goal);
    }

    @Transactional
    public String delete(Integer studentId, Integer goalId) {
        CareerGoal goal = ownedGoal(studentId, goalId);
        String title = goal.getTitle();
        goal.getStudent().getGoals().remove(goal);      // composition: tasks are removed with the goal
        goals.delete(goal);
        return title;
    }

    @Transactional
    public Result<GoalTask> addTask(Integer studentId, Integer goalId, String title) {
        CareerGoal goal = ownedGoal(studentId, goalId);
        String t = title == null ? "" : title.strip();
        if (t.isEmpty() || t.length() > 200) {
            return Result.fail("A task needs a title of 1-200 characters.");
        }
        if (goal.getTasks().size() >= 50) {
            return Result.fail("A goal can have at most 50 tasks.");
        }
        GoalTask task = new GoalTask(t, false);
        goal.addTask(task);
        tasks.save(task);
        return Result.ok(task);
    }

    /** Mark a task complete or incomplete. */
    @Transactional
    public GoalTask setDone(Integer studentId, Integer taskId, boolean done) {
        GoalTask task = ownedTask(studentId, taskId);
        boolean wasDone = task.isDone();
        task.setDone(done);
        if (done && !wasDone) {
            Student s = task.getGoal().getStudent();
            activity.record(s, "Task completed", task.getTitle());
        }
        return task;
    }

    @Transactional
    public void deleteTask(Integer studentId, Integer taskId) {
        GoalTask task = ownedTask(studentId, taskId);
        task.getGoal().getTasks().remove(task);
        tasks.delete(task);
    }

    /** Goal counts, overall task progress and the next pending task. */
    public Summary summary(List<CareerGoal> list) {
        List<GoalTask> all = list.stream().flatMap(g -> g.getTasks().stream()).toList();
        int done = (int) all.stream().filter(GoalTask::isDone).count();
        Optional<GoalTask> next = all.stream().filter(t -> !t.isDone()).findFirst();
        return new Summary(list.size(), all.size(), done, all.size() - done,
                all.isEmpty() ? 0 : (int) Math.round(100.0 * done / all.size()), next.map(GoalTask::getTitle).orElse(null));
    }

    private static String checkGoal(String title, String description) {
        String t = title == null ? "" : title.strip();
        if (t.length() < 3 || t.length() > 160) {
            return "Please enter a goal title of 3-160 characters.";
        }
        if (description != null && description.strip().length() > 500) {
            return "The goal description must be at most 500 characters.";
        }
        return null;
    }

    private CareerGoal ownedGoal(Integer studentId, Integer goalId) {
        return goals.findById(goalId).filter(g -> g.getStudent().getId().equals(studentId))
                .orElseThrow(() -> new NotFoundException("Goal not found."));
    }

    private GoalTask ownedTask(Integer studentId, Integer taskId) {
        return tasks.findById(taskId).filter(t -> t.getGoal().getStudent().getId().equals(studentId))
                .orElseThrow(() -> new NotFoundException("Task not found."));
    }
}
