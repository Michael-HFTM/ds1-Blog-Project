package ch.diamondh3art.blog.boundry;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/blog")
public class BlogResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Willkommen auf meinem Blog!";
    }
}