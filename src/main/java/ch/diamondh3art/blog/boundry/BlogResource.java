package ch.diamondh3art.blog.boundry;

import ch.diamondh3art.blog.boundry.dto.BlogDto;
import ch.diamondh3art.blog.boundry.dto.NewBlogDto;
import ch.diamondh3art.blog.boundry.dto.UpdateBlogDto;
import ch.diamondh3art.blog.control.BlogService;
import ch.diamondh3art.blog.entity.Blog;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Optional;

@Path("/blogs")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Blogs")
@DenyAll
public class BlogResource {

    /** Every error response carries this body, so the mappings are annotated once here. */
    private static final String ERROR = "ErrorResponse";

    @Inject
    BlogService blogService;

    @Inject
    SecurityIdentity securityIdentity;

    @GET
    @Operation(summary = "List blogs, optionally filtered by searchString")
    // PagedResponse<BlogDto> is erased at runtime, so the item schema has to be spelled out
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(name = "BlogPage",
            properties = {
                    @SchemaProperty(name = "data", type = SchemaType.ARRAY, implementation = BlogDto.class),
                    @SchemaProperty(name = "meta", implementation = PagedResponse.PageMeta.class)
            })))
    @APIResponse(responseCode = "400", ref = ERROR)
    @PermitAll
    public Response getEntries(
            @QueryParam("searchString") Optional<String> searchString,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        PagedResponse<Blog> blogs = blogService.getBlogs(searchString, page, size);
        return Response.ok(new PagedResponse<>(BlogDto.from(blogs.data()), blogs.meta())).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a single blog")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = BlogDto.class)))
    @APIResponse(responseCode = "404", ref = ERROR)
    @PermitAll
    public Response getEntry(@PathParam("id") Long id) {
        return Response.ok(BlogDto.from(blogService.getBlog(id))).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a blog")
    @APIResponse(responseCode = "201", description = "Location header points at the new blog",
            content = @Content(schema = @Schema(implementation = BlogDto.class)))
    @APIResponse(responseCode = "400", ref = ERROR)
    @APIResponse(responseCode = "401", ref = ERROR)
    @APIResponse(responseCode = "403", ref = ERROR)
    @RolesAllowed({"author", "admin"})
    public Response addBlog(@Valid NewBlogDto newBlogDto, @Context UriInfo uriInfo) {
        // @Valid does not fire on a completely missing body
        if (newBlogDto == null) {
            throw ResponseException.badRequest("Request-Body fehlt");
        }

        Blog blog = newBlogDto.toBlog();
        blog.setAuthor(securityIdentity.getPrincipal().getName());
        Blog created = blogService.addBlog(blog);

        // tell the client where the new resource lives
        UriBuilder location = uriInfo.getAbsolutePathBuilder().path(Long.toString(created.getId()));
        return Response.created(location.build()).entity(BlogDto.from(created)).build();
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Partially update a blog",
            description = "Only the fields present in the body are changed, e.g. {\"content\": "
                    + "\"Neuer Inhalt\"}. The merged blog is validated, so a blank title is "
                    + "rejected and the stored blog stays untouched.")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = BlogDto.class)))
    @APIResponse(responseCode = "400", ref = ERROR)
    @APIResponse(responseCode = "401", ref = ERROR)
    @APIResponse(responseCode = "403", ref = ERROR)
    @APIResponse(responseCode = "404", ref = ERROR)
    @RolesAllowed({"author", "admin"})
    public Response updateBlog(@PathParam("id") Long id, @Valid UpdateBlogDto updateBlogDto) {
        if (updateBlogDto == null) {
            throw ResponseException.badRequest("Request-Body fehlt");
        }

        // @RolesAllowed cannot express "own blogs only"
        String username = securityIdentity.getPrincipal().getName();
        if (!securityIdentity.hasRole("admin") && !blogService.isAuthor(id, username)) {
            throw ResponseException.forbidden("Du darfst nur deine eigenen Blogs bearbeiten");
        }

        return Response.ok(BlogDto.from(blogService.updateBlog(id, updateBlogDto.toBlogPatch())))
                .build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a blog together with its comments")
    @APIResponse(responseCode = "204", description = "Deleted")
    @APIResponse(responseCode = "401", ref = ERROR)
    @APIResponse(responseCode = "403", ref = ERROR)
    @APIResponse(responseCode = "404", ref = ERROR)
    @RolesAllowed("admin")
    public Response deleteBlog(@PathParam("id") Long id) {
        blogService.deleteBlog(id);
        return Response.noContent().build();
    }

}
