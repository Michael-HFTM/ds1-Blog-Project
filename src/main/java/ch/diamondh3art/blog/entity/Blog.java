package ch.diamondh3art.blog.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;

// not part of the API surface - clients see the DTOs in boundry.dto instead
@Entity
public class Blog {

    @Id
    @GeneratedValue
    private Long id;

    @NotBlank(message = "Titel darf nicht leer sein.")
    private String title;

    @NotBlank(message = "Inhalt darf nicht leer sein.")
    private String content;

    // username of the creator; null means no owner, so only an admin can edit the blog
    private String author;

    public Blog() {
    }

    public Blog(String title, String content) {
        this.content = content;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
