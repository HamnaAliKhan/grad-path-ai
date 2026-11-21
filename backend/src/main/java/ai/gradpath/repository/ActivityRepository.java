package ai.gradpath.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Limit;

import ai.gradpath.domain.ActivityLog;

/** Recent activity events. */
public interface ActivityRepository extends BaseRepository<ActivityLog> {

    List<ActivityLog> findAllByOrderByIdDesc(Limit limit);

    List<ActivityLog> findByUserIdOrderByIdDesc(Integer userId, Limit limit);

    List<ActivityLog> findByActionInOrderByIdDesc(Collection<String> actions, Limit limit);

    default List<ActivityLog> recent(Integer userId, int limit) {
        return userId == null ? findAllByOrderByIdDesc(Limit.of(limit))
                : findByUserIdOrderByIdDesc(userId, Limit.of(limit));
    }

    default List<ActivityLog> recentProfiles(int limit) {
        return findByActionInOrderByIdDesc(List.of("Profile created", "Profile completed"), Limit.of(limit));
    }
}
