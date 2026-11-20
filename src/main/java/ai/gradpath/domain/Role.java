package ai.gradpath.domain;

/** Account roles. The stored value is the lower-case code in users.role. */
public enum Role {
    STUDENT("student", "Student"),
    OFFICER("officer", "Career Officer");

    private final String code;
    private final String label;

    Role(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    /** Spring Security authority name, e.g. ROLE_STUDENT. */
    public String authority() {
        return "ROLE_" + name();
    }

    public static Role fromCode(String code) {
        for (Role r : values()) {
            if (r.code.equalsIgnoreCase(code)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + code);
    }
}
