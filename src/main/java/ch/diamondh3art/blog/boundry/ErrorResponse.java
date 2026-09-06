package ch.diamondh3art.blog.boundry;

import jakarta.ws.rs.core.Response;

/**
 * Body of every error response, so a client can read the reason instead of getting an empty body.
 */
public record ErrorResponse(int status, String error, String message) {

    public static ErrorResponse of(Response.Status status, String message) {
        return new ErrorResponse(status.getStatusCode(), status.getReasonPhrase(), message);
    }
}
