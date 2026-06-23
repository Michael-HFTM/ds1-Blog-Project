package ch.diamondh3art.blog.control;

import ch.diamondh3art.blog.entity.Comment;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class CommentService {
    @Inject
    CommentRepository commentRepository;

    public List<Comment> getComments() {
        var comments = commentRepository.listAll();
        Log.info("Returning " + comments.size() + " blogs");
        return comments;
    }

    @Transactional
    public void addComment(Comment comment) {
        Log.info("Adding comment " + comment.getId());
        commentRepository.persist(comment);
    }
}