package ch.diamondh3art.blog.boundry;

import jakarta.validation.ConstraintViolation;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Set;

/**
 * Exception that already carries the whole error response, so the error format is defined once.
 */
public class ResponseException extends WebApplicationException {

    public ResponseException(Response.Status status, String message) {
        this(status, message, ErrorResponse.of(status, message));
    }

    private ResponseException(Response.Status status, String message, ErrorResponse body) {
        super(message, errorResponse(status, message, body));
    }

    public static ResponseException badRequest(String message) {
        return new ResponseException(Response.Status.BAD_REQUEST, message);
    }

    public static ResponseException notFound(String message) {
        return new ResponseException(Response.Status.NOT_FOUND, message);
    }

    public static ResponseException forbidden(String message) {
        return new ResponseException(Response.Status.FORBIDDEN, message);
    }

    /**
     * A plain ConstraintViolationException would be mapped to 500 by Quarkus, because Bean
     * Validation treats violations outside of parameter validation as a server error. Wrapping
     * them here keeps the 400 and the API's error format.
     */
    public static ResponseException validationFailed(Set<? extends ConstraintViolation<?>> violations) {
        return new ResponseException(Response.Status.BAD_REQUEST, VALIDATION_FAILED,
                ErrorResponse.ofViolations(Response.Status.BAD_REQUEST, violations));
    }

    static final String VALIDATION_FAILED = "Validation failed";

    static Response errorResponse(Response.Status status, String message) {
        return errorResponse(status, message, ErrorResponse.of(status, message));
    }

    private static Response errorResponse(Response.Status status, String message, ErrorResponse body) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .header("info", message)
                .entity(body)
                .build();
    }
}
