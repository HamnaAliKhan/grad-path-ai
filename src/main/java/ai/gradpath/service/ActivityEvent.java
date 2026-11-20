package ai.gradpath.service;

/**
 * Something a user did that should appear in activity feeds (e.g. "Applied for a job", "Resume analysed").
 * OBSERVER PATTERN: services publish these events; {@link ActivityService#onActivity} observes and stores them,
 * so publishers do not depend on how activity is recorded.
 */
public record ActivityEvent(Integer userId, String actorName, String action, String detail) {
}
