package ai.gradpath.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** A student's career goal. COMPOSITION: its tasks are created and deleted with it. */
@Entity
@Table(name = "career_goals")
public class CareerGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(length = 160, nullable = false)
    private String title;
    @Column(nullable = false, columnDefinition = "text")
    private String description = "";
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = Csv.utcNow();

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<GoalTask> tasks = new ArrayList<>();

    public CareerGoal() { }

    public CareerGoal(String title, String description) {
        this.title = title;
        this.description = description;
    }

    /** Percentage of completed tasks (0 when the goal has no tasks yet). */
    public int progress() {
        if (tasks.isEmpty()) {
            return 0;
        }
        return (int) Math.round(100.0 * doneCount() / tasks.size());
    }

    public long doneCount() {
        return tasks.stream().filter(GoalTask::isDone).count();
    }

    public void addTask(GoalTask task) {
        task.setGoal(this);
        tasks.add(task);
    }

    public Integer getId() { return id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<GoalTask> getTasks() { return tasks; }
}
