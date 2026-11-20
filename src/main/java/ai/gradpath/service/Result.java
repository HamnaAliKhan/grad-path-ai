package ai.gradpath.service;

import java.util.List;

/**
 * Outcome of an operation: a value, or one or more user-facing error messages.
 *
 * @param <T> value type
 */
public record Result<T>(T value, List<String> errors) {

    public Result {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static <T> Result<T> ok(T value) {
        return new Result<>(value, List.of());
    }

    public static <T> Result<T> fail(String error) {
        return new Result<>(null, List.of(error));
    }

    public static <T> Result<T> fail(List<String> errors) {
        return new Result<>(null, errors);
    }

    public boolean isOk() {
        return errors.isEmpty();
    }

    /** The first error, or an empty string. */
    public String error() {
        return errors.isEmpty() ? "" : errors.get(0);
    }
}
