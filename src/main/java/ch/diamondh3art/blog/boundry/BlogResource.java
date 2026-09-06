package ch.diamondh3art.blog.boundry;

import ch.diamondh3art.blog.control.BlogService;
import ch.diamondh3art.blog.entity.Blog;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.util.Optional;

@Path("/blogs")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class BlogResource {

    @Inject
    BlogService blogService;

    @GET
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    @APIResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response getEntries(
            @QueryParam("searchString") Optional<String> searchString,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return Response.ok(blogService.getBlogs(searchString, page, size)).build();
    }

    @GET
    @Path("/{id}")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Blog.class)))
    @APIResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response getEntry(@PathParam("id") Long id) {
        return Response.ok(blogService.getBlog(id)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = Blog.class)))
    @APIResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response addBlog(Blog blog, @Context UriInfo uriInfo) {
        // TODO: replace with Bean Validation once we covered it in class
        if (blog == null || blog.getTitle() == null || blog.getTitle().isBlank()) {
            throw ResponseException.badRequest("Request-Body oder Titel fehlt");
        }

        Blog created = blogService.addBlog(blog);

        // tell the client where the new resource lives
        UriBuilder location = uriInfo.getAbsolutePathBuilder().path(Long.toString(created.getId()));
        return Response.created(location.build()).entity(created).build();
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Blog.class)))
    @APIResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response updateBlog(@PathParam("id") Long id, Blog blog) {
        if (blog == null) {
            throw ResponseException.badRequest("Request-Body fehlt");
        }

        return Response.ok(blogService.updateBlog(id, blog)).build();
    }

    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "204")
    @APIResponse(responseCode = "403", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response deleteBlog(@PathParam("id") Long id, @HeaderParam("secret") String secret) {
        blogService.deleteBlog(id, secret);
        return Response.noContent().build();
    }

}
