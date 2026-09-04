package ch.diamondh3art.blog.boundry;

import ch.diamondh3art.blog.control.BlogService;
import ch.diamondh3art.blog.entity.Blog;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;
import java.util.Optional;

@Path("/blogs")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class BlogResource {

    @Inject
    BlogService blogService;

    @GET
    public List<Blog> getEntries(@QueryParam("searchString") Optional<String> searchString) {
        return blogService.getBlogs(searchString);
    }

    @GET
    @Path("/{id}")
    public Blog getEntry(@PathParam("id") Long id) {
        return blogService.getBlog(id);
    }

    @POST
    @ResponseStatus(201)
    @Consumes(MediaType.APPLICATION_JSON)
    public Blog addBlog(Blog blog) {
        return blogService.addBlog(blog);
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Blog updateBlog(@PathParam("id") Long id, Blog blog) {
        return blogService.updateBlog(id, blog);
    }

    @DELETE
    @Path("/{id}")
    public void deleteBlog(@PathParam("id") Long id, @HeaderParam("secret") String secret) {
        blogService.deleteBlog(id, secret);
    }

}