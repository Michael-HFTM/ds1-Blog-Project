-- Initiale Tabellenstruktur der Blog-Applikation, Tabellennamen exakt wie die Entitäten (MySQL unter Linux ist case-sensitiv)
CREATE TABLE Blog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    author VARCHAR(255)
);

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255)
);

CREATE TABLE Comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message VARCHAR(255),
    blog_id BIGINT,
    author_id BIGINT,
    CONSTRAINT fk_comment_blog FOREIGN KEY (blog_id) REFERENCES Blog (id),
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES users (id)
);
