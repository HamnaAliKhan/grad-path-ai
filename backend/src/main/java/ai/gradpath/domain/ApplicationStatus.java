package ai.gradpath.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Application review states, in workflow order. The label is the value stored in job_applications.status. */
public enum ApplicationStatus {
    SUBMITTED("Submitted"),
    UNDER_REVIEW("Under review"),
    SHORTLISTED("Shortlisted"),
    INTERVIEW("Interview"),
    OFFER("Offer"),
    NOT_SELECTED("Not selected");

    private final String label;

    ApplicationStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** All stored labels in workflow order. */
    public static List<String> labels() {
        return Arrays.stream(values()).map(ApplicationStatus::label).toList();
    }

    public static Optional<ApplicationStatus> fromLabel(String label) {
        return Arrays.stream(values()).filter(s -> s.label.equals(label)).findFirst();
    }

    /** Shortlisted, Interview or Offer. */
    public boolean isPositive() {
        return this == SHORTLISTED || this == INTERVIEW || this == OFFER;
    }
}
