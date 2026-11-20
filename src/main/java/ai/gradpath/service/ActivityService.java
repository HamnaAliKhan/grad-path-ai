package ai.gradpath.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.gradpath.domain.ActivityLog;
import ai.gradpath.domain.User;
import ai.gradpath.repository.ActivityRepository;

/** Publishes and stores activity events (OBSERVER pattern via Spring application events). */
@Service
public class ActivityService {

    private final ApplicationEventPublisher events;
    private final ActivityRepository activity;

    public ActivityService(ApplicationEventPublisher events, ActivityRepository activity) {
        this.events = events;
        this.activity = activity;
    }

    /** Publish an activity event for a user. */
    public void record(User user, String action, String detail) {
        events.publishEvent(new ActivityEvent(user.getId(), user.getName(), action, detail));
    }

    /** Observer: persist each published activity event (runs in the publisher's transaction). */
    @EventListener
    @Transactional
    public void onActivity(ActivityEvent e) {
        activity.save(new ActivityLog(e.userId(), e.actorName(), e.action(), e.detail()));
    }

    @Transactional(readOnly = true)
    public List<ActivityLog> recent(Integer userId, int limit) {
        return activity.recent(userId, limit);
    }

    @Transactional(readOnly = true)
    public List<ActivityLog> recentProfiles(int limit) {
        return activity.recentProfiles(limit);
    }
}
