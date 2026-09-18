package ch.diamondh3art.blog.boundry;

import ch.diamondh3art.blog.boundry.dto.BlogDto;
import ch.diamondh3art.blog.boundry.dto.NewBlogDto;
import ch.diamondh3art.blog.boundry.dto.UpdateBlogDto;
import ch.diamondh3art.blog.control.BlogService;
import ch.diamondh3art.blog.entity.Blog;
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
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Optional;

@Path("/blogs")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Blogs", description = "Create, read, update and delete blog posts.")
public class BlogResource {

    @Inject
    BlogService blogService;

    @GET
    @Operation(summary = "List blogs",
            description = "Returns a page of blogs wrapped in a data property, together with meta "
                    + "data such as totalCount and pageCount. An optional searchString filters by "
                    + "title or content; the meta data then refers to the filtered result.")
    // the generic type of PagedResponse<Blog> is erased, so the item schema is spelled out here
    @APIResponse(responseCode = "200", description = "A page of blogs",
            content = @Content(schema = @Schema(name = "BlogPage",
                    description = "A page of blogs together with its meta data.",
                    properties = {
                            @SchemaProperty(name = "data", type = SchemaType.ARRAY,
                                    implementation = BlogDto.class),
                            @SchemaProperty(name = "meta",
                                    implementation = PagedResponse.PageMeta.class)
                    })))
    @APIResponse(responseCode = "400", description = "page is negative, or size is outside 1..100",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response getEntries(
            @Parameter(description = "Case-insensitive substring match on title or content",
                    example = "jedi")
            @QueryParam("searchString") Optional<String> searchString,

            @Parameter(description = "Zero-based page index. A page beyond the last one returns an "
                    + "empty data array rather than a 404.", example = "0")
            @QueryParam("page") @DefaultValue("0") int page,

            @Parameter(description = "Records per page, between 1 and 100", example = "20")
            @QueryParam("size") @DefaultValue("20") int size) {
        PagedResponse<Blog> blogs = blogService.getBlogs(searchString, page, size);
        return Response.ok(new PagedResponse<>(BlogDto.from(blogs.data()), blogs.meta())).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a single blog")
    @APIResponse(responseCode = "200", description = "The blog",
            content = @Content(schema = @Schema(implementation = BlogDto.class)))
    @APIResponse(responseCode = "404", description = "No blog with the given id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response getEntry(
            @Parameter(description = "Id of the blog", required = true, example = "1")
            @PathParam("id") Long id) {
        return Response.ok(BlogDto.from(blogService.getBlog(id))).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a blog")
    @RequestBody(required = true, description = "Title and content of the new blog")
    @APIResponse(responseCode = "201", description = "The created blog",
            headers = @Header(name = "Location", description = "URI of the created blog",
                    schema = @Schema(type = SchemaType.STRING)),
            content = @Content(schema = @Schema(implementation = BlogDto.class)))
    @APIResponse(responseCode = "400", description = "Missing body, or title/content is blank",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response addBlog(@Valid NewBlogDto newBlogDto, @Context UriInfo uriInfo) {
        // @Valid does not fire on a completely missing body
        if (newBlogDto == null) {
            throw ResponseException.badRequest("Request-Body fehlt");
        }

        Blog created = blogService.addBlog(newBlogDto.toBlog());

        // tell the client where the new resource lives
        UriBuilder location = uriInfo.getAbsolutePathBuilder().path(Long.toString(created.getId()));
        return Response.created(location.build()).entity(BlogDto.from(created)).build();
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Partially update a blog",
            description = "Only the fields present in the body are changed; omitted fields keep "
                    + "their value. The merged blog is validated, so a blank title is rejected and "
                    + "the stored blog stays untouched.")
    @RequestBody(required = true, content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            examples = {
                    @ExampleObject(name = "Nur Inhalt ändern", value = "{\"content\": \"Neuer Inhalt\"}"),
                    @ExampleObject(name = "Nur Titel ändern", value = "{\"title\": \"Neuer Titel\"}")
            }))
    @APIResponse(responseCode = "200", description = "The updated blog",
            content = @Content(schema = @Schema(implementation = BlogDto.class)))
    @APIResponse(responseCode = "400", description = "Missing body, or the merged blog is invalid",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "No blog with the given id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response updateBlog(
            @Parameter(description = "Id of the blog to update", required = true, example = "1")
            @PathParam("id") Long id,
            @Valid UpdateBlogDto updateBlogDto) {
        
        if (updateBlogDto == null) {
            throw ResponseException.badRequest("Request-Body fehlt");
        }

        return Response.ok(BlogDto.from(blogService.updateBlog(id, updateBlogDto.toBlogPatch())))
                .build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a blog",
            description = "Deletes the blog together with its comments. Requires the shared secret "
                    + "in the secret header.")
    @APIResponse(responseCode = "204", description = "Deleted, no content returned")
    @APIResponse(responseCode = "403", description = "Wrong or missing secret header",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "No blog with the given id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response deleteBlog(
            @Parameter(description = "Id of the blog to delete", required = true, example = "1")
            @PathParam("id") Long id,

            @Parameter(description = "Shared secret, see blog.delete.secret in application.properties",
                    required = true)
            @HeaderParam("secret") String secret) {
        blogService.deleteBlog(id, secret);
        return Response.noContent().build();
    }

}
