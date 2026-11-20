package ai.gradpath.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import ai.gradpath.domain.Job;

/** Job listing search. */
public interface JobRepository extends BaseRepository<Job> {

    /** Filter by a lower-case LIKE pattern (empty = any), category and level ("All" = any) and open status. */
    @Query("""
            select j from Job j
            where (:onlyOpen = false or j.open = true)
              and (:pattern = '' or lower(j.title) like :pattern or lower(j.company) like :pattern)
              and (:category = 'All' or j.category = :category)
              and (:level = 'All' or j.level = :level)
            order by j.createdAt desc, j.id desc
            """)
    List<Job> filter(String pattern, String category, String level, boolean onlyOpen);

    /** Search by free text (title or company), category, level and open status. */
    default List<Job> search(String query, String category, String level, boolean onlyOpen) {
        String q = query == null ? "" : query.strip().toLowerCase().replace("%", "").replace("_", "");
        return filter(q.isEmpty() ? "" : "%" + q + "%",
                category == null || category.isBlank() ? "All" : category,
                level == null || level.isBlank() ? "All" : level, onlyOpen);
    }

    long countByOpenTrue();
}
