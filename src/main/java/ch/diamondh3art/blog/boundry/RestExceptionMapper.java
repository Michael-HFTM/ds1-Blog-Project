package ch.diamondh3art.blog.boundry;

import io.quarkus.logging.Log;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Safety net so that errors raised outside our own code (malformed JSON, a non-numeric query
 * parameter, ...) are reported in the same format as a {@link ResponseException}.
 */
@Provider
public class RestExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof WebApplicationException webAppException) {
            Response response = webAppException.getResponse();
            if (response.hasEntity()) {
                // already carries an ErrorResponse, e.g. thrown as ResponseException
                return response;
            }
            return errorResponse(statusOf(response.getStatus()), exception.getMessage());
        }

        if (exception instanceof IllegalArgumentException) {
            return errorResponse(Response.Status.BAD_REQUEST, exception.getMessage());
        }

        Log.error("Unhandled exception", exception);
        return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }

    private static Response.Status statusOf(int statusCode) {
        Response.Status status = Response.Status.fromStatusCode(statusCode);
        return status != null ? status : Response.Status.INTERNAL_SERVER_ERROR;
    }

    private static Response errorResponse(Response.Status status, String message) {
        return ResponseException.errorResponse(status, message != null ? message : status.getReasonPhrase());
    }
}
