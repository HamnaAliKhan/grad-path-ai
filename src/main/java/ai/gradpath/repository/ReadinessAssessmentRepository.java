package ai.gradpath.repository;

import java.util.List;
import java.util.Optional;

import ai.gradpath.domain.ReadinessAssessment;

/** Saved readiness results. */
public interface ReadinessAssessmentRepository extends BaseRepository<ReadinessAssessment> {

    /** The most recent rule-engine result (legacy ML-era rows are ignored). */
    Optional<ReadinessAssessment> findFirstByStudentIdAndLegacyFalseOrderByIdDesc(Integer studentId);

    /** Rule-engine results from oldest to newest. */
    List<ReadinessAssessment> findByStudentIdAndLegacyFalseOrderById(Integer studentId);

    long countByStudentIdAndLegacyTrue(Integer studentId);
}
