package ch.diamondh3art.blog.entity;

import jakarta.persistence.*;

@Entity
public class Comment {

    @Id
    @GeneratedValue
    private Long id;

    private String message;

    @ManyToOne
    private Blog blog;
    @ManyToOne
    private User author;

    public Comment() {
    }

    public Comment(String message, Blog blog, User author) {
        this.message = message;
        this.blog = blog;
        this.author = author;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Blog getBlog() {
        return blog;
    }

    public void setBlog(Blog blog) {
        this.blog = blog;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }
}
