package ch.diamondh3art.blog.boundry;

import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Maps @Valid failures into the same error format as the rest of the API instead of using the
 * Quarkus default representation.
 */
public class ValidationExceptionMapper {

    @ServerExceptionMapper(ResteasyReactiveViolationException.class)
    public Response handleViolationException(ResteasyReactiveViolationException exception) {
        return ResponseException.validationFailed(exception.getConstraintViolations()).getResponse();
    }
}
