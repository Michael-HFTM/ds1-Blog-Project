package ch.diamondh3art.blog.boundry;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Exception that already carries the whole error response, so the error format is defined once.
 */
public class ResponseException extends WebApplicationException {

    public ResponseException(Response.Status status, String message) {
        super(message, errorResponse(status, message));
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

    static Response errorResponse(Response.Status status, String message) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .header("info", message)
                .entity(ErrorResponse.of(status, message))
                .build();
    }
}
