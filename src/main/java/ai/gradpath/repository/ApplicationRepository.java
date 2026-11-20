package ai.gradpath.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import ai.gradpath.domain.JobApplication;

/** Application queries; resume bytes are loaded only on demand. */
public interface ApplicationRepository extends BaseRepository<JobApplication> {

    /** Applications for a job, best fit first. */
    @Query("select a from JobApplication a where a.job.id = :jobId order by a.fitScore desc, a.createdAt")
    List<JobApplication> forJob(Integer jobId);

    /** A student's applications, newest first. */
    @Query("select a from JobApplication a where a.student.id = :studentId order by a.createdAt desc")
    List<JobApplication> forStudent(Integer studentId);

    @Query("select a.job.id from JobApplication a where a.student.id = :studentId")
    Set<Integer> jobIdsForStudent(Integer studentId);

    @Query("select a from JobApplication a where a.job.id = :jobId and a.student.id = :studentId")
    Optional<JobApplication> find(Integer jobId, Integer studentId);

    /** Every application, best fit first. */
    @Query("select a from JobApplication a order by a.fitScore desc, a.createdAt")
    List<JobApplication> everything();

    long countByStatus(String status);

    @Query("select a.job.id, count(a) from JobApplication a group by a.job.id")
    List<Object[]> countRows();

    /** Application counts keyed by job id. */
    default Map<Integer, Long> countsByJob() {
        Map<Integer, Long> out = new LinkedHashMap<>();
        for (Object[] row : countRows()) {
            out.put((Integer) row[0], (Long) row[1]);
        }
        return out;
    }

    /** DEFERRED LOADING: one application's resume PDF, fetched only when an authorised officer opens it. */
    @Query(value = "select resume_pdf from job_applications where id = :id", nativeQuery = true)
    byte[] resumePdf(Integer id);

    @Modifying(flushAutomatically = true)
    @Query(value = "update job_applications set resume_pdf = :pdf where id = :id", nativeQuery = true)
    void storeResumePdf(Integer id, byte[] pdf);
}
