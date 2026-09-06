package ch.diamondh3art.blog.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Blog {

    @Id
    @GeneratedValue
    private Long id;

    @NotBlank(message = "Titel darf nicht leer sein.")
    private String title;

    @NotBlank(message = "Inhalt darf nicht leer sein.")
    private String content;

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
}
