package ai.gradpath.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.gradpath.domain.ReadinessAssessment;
import ai.gradpath.domain.Student;
import ai.gradpath.readiness.ProfileSnapshot;
import ai.gradpath.readiness.ReadinessResult;
import ai.gradpath.readiness.ReadinessRuleEngine;
import ai.gradpath.repository.ReadinessAssessmentRepository;

/**
 * FACADE over the rule engine and the readiness history: controllers ask for a student's result, a what-if result
 * (never saved) or the saved history without knowing about snapshots, rules or repositories.
 */
@Service
public class ReadinessService {

    private final ReadinessRuleEngine engine;
    private final ReadinessAssessmentRepository history;

    public ReadinessService(ReadinessRuleEngine engine, ReadinessAssessmentRepository history) {
        this.engine = engine;
        this.history = history;
    }

    /** The current rule-based result for a saved profile. */
    public ReadinessResult evaluate(Student student) {
        return engine.evaluate(ProfileSnapshot.of(student));
    }

    /** Score an unsaved variation of a profile (what-if simulator). Nothing is stored. */
    public ReadinessResult evaluate(ProfileSnapshot snapshot) {
        return engine.evaluate(snapshot);
    }

    /** Save the current result when it differs from the last saved one (score, category, version or status). */
    @Transactional
    public ReadinessResult record(Student student) {
        ReadinessResult r = evaluate(student);
        Optional<ReadinessAssessment> last = history.findFirstByStudentIdAndLegacyFalseOrderByIdDesc(student.getId());
        boolean changed = last.isEmpty()
                || Math.abs(last.get().getScore() - r.score()) > 0.01
                || !last.get().getRuleEngineVersion().equals(r.engineVersion())
                || last.get().isPreliminary() != r.preliminary();
        if (changed) {
            history.save(new ReadinessAssessment(student.getId(), r.score(), r.category().label(),
                    ReadinessRuleEngine.ENGINE_NAME, r.engineVersion(), r.preliminary()));
        }
        return r;
    }

    /** Saved rule-engine results, oldest first (legacy ML-era rows excluded). */
    @Transactional(readOnly = true)
    public List<ReadinessAssessment> history(Integer studentId) {
        return history.findByStudentIdAndLegacyFalseOrderById(studentId);
    }

    /** Number of earlier (pre rule-engine) records kept for this student. */
    @Transactional(readOnly = true)
    public long legacyCount(Integer studentId) {
        return history.countByStudentIdAndLegacyTrue(studentId);
    }

    public ReadinessRuleEngine engine() {
        return engine;
    }
}
