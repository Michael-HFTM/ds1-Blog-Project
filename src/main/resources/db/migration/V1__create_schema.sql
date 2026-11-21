-- Schema passend zu den Entities Blog, User und Comment; IDs über Hibernate-Sequenztabellen (allocationSize 50)
CREATE TABLE Blog (
    id      BIGINT       NOT NULL,
    title   VARCHAR(255) NOT NULL,
    content VARCHAR(255) NOT NULL,
    author  VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE users (
    id    BIGINT NOT NULL,
    name  VARCHAR(255),
    email VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE Comment (
    id        BIGINT NOT NULL,
    message   VARCHAR(255),
    blog_id   BIGINT,
    author_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_comment_blog FOREIGN KEY (blog_id) REFERENCES Blog (id),
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE TABLE Blog_SEQ (next_val BIGINT);
INSERT INTO Blog_SEQ VALUES (1);
CREATE TABLE users_SEQ (next_val BIGINT);
INSERT INTO users_SEQ VALUES (1);
CREATE TABLE Comment_SEQ (next_val BIGINT);
INSERT INTO Comment_SEQ VALUES (1);
