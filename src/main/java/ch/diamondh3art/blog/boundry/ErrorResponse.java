package ch.diamondh3art.blog.boundry;

import jakarta.validation.ConstraintViolation;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.Set;

/**
 * Body of every error response, so a client can read the reason instead of getting an empty body.
 * {@code violations} is only filled for validation errors and omitted otherwise.
 */
@Schema(name = "ErrorResponse", description = "Uniform body of every error response.")
public record ErrorResponse(
        @Schema(description = "HTTP status code", examples = {"400"})
        int status,
        @Schema(description = "Reason phrase of the status code", examples = {"Bad Request"})
        String error,
        @Schema(description = "Human readable reason, also sent in the info header",
                examples = {"Validation failed"})
        String message,
        @Schema(description = "Only present for validation errors", nullable = true)
        List<Violation> violations) {

    @Schema(name = "Violation", description = "A single failed constraint.")
    public record Violation(
            @Schema(description = "Name of the rejected field", examples = {"title"})
            String field,
            @Schema(description = "Why it was rejected", examples = {"Titel darf nicht leer sein."})
            String message) {
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
