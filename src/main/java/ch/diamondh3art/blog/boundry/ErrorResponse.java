package ch.diamondh3art.blog.boundry;

import jakarta.validation.ConstraintViolation;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Set;

/**
 * Body of every error response, so a client can read the reason instead of getting an empty body.
 * {@code violations} is only filled for validation errors and omitted otherwise.
 */
public record ErrorResponse(int status, String error, String message, List<Violation> violations) {

    public record Violation(String field, String message) {
    }

    public static ErrorResponse of(Response.Status status, String message) {
        return new ErrorResponse(status.getStatusCode(), status.getReasonPhrase(), message, null);
    }

    public static ErrorResponse ofViolations(Response.Status status, Set<? extends ConstraintViolation<?>> violations) {
        List<Violation> fields = violations.stream()
                .map(violation -> new Violation(fieldOf(violation), violation.getMessage()))
                .sorted((left, right) -> left.field().compareTo(right.field()))
                .toList();

        return new ErrorResponse(status.getStatusCode(), status.getReasonPhrase(), "Validation failed", fields);
    }

    /**
     * Bean Validation reports a property path like {@code addBlog.blog.title}; a client only cares
     * about the last segment.
     */
    private static String fieldOf(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot < 0 ? path : path.substring(lastDot + 1);
    }
}
