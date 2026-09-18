package ch.diamondh3art.blog.boundry;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

/**
 * Declares the ErrorResponse component once, so an endpoint can document an error with
 * {@code @APIResponse(responseCode = "404", ref = ERROR)} instead of repeating the schema.
 * The empty info is only there because the annotation requires it; title, version and
 * description come from application.properties and win over these values.
 */
@ApplicationPath("/")
@OpenAPIDefinition(
        info = @Info(title = "", version = ""),
        components = @Components(responses = @APIResponse(name = "ErrorResponse",
                description = "Error, see message for the reason",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                        schema = @Schema(implementation = ErrorResponse.class)))))
public class RestApplication extends Application {
}
