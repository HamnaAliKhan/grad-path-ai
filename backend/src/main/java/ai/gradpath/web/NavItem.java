package ai.gradpath.web;

/** One sidebar link. */
public record NavItem(String label, String href, String icon, long badge, boolean active) {
}
