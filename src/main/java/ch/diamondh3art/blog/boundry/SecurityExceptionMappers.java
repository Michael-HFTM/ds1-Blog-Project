package ch.diamondh3art.blog.boundry;

import io.quarkus.logging.Log;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Failed {@code @RolesAllowed} checks would otherwise answer with Quarkus' empty default body
 * instead of the API's error format.
 */
public class SecurityExceptionMappers {

    @ServerExceptionMapper
    public Response unauthorized(UnauthorizedException exception) {
        Log.debug("Rejected unauthenticated request to a protected endpoint");
        return Response.fromResponse(ResponseException.errorResponse(Response.Status.UNAUTHORIZED, "Login required"))
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .build();
    }

    @ServerExceptionMapper
    public Response forbidden(ForbiddenException exception) {
        Log.warn("Rejected request without the required role");
        return ResponseException.errorResponse(Response.Status.FORBIDDEN, "Missing required role");
    }
}
