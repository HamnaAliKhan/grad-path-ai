package ai.gradpath.readiness;

/** Raised when a readiness input is outside its documented range. The message is safe to show to users. */
public class InvalidProfileException extends RuntimeException {

    private final String field;

    public InvalidProfileException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
