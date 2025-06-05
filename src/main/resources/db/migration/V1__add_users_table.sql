CREATE SEQUENCE IF NOT EXISTS user_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE users
(
    id       BIGINT      NOT NULL,
    name     VARCHAR(20) NOT NULL,
    password TEXT        NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE INDEX users_name ON users (name);