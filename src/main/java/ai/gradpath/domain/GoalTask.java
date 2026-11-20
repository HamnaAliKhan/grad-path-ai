package ai.gradpath.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One actionable task within a career goal. */
@Entity
@Table(name = "goal_tasks")
public class GoalTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private CareerGoal goal;

    @Column(length = 200, nullable = false)
    private String title;
    @Column(nullable = false)
    private boolean done;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = Csv.utcNow();

    public GoalTask() { }

    public GoalTask(String title, boolean done) {
        this.title = title;
        this.done = done;
    }

    public Integer getId() { return id; }
    public CareerGoal getGoal() { return goal; }
    public void setGoal(CareerGoal goal) { this.goal = goal; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
