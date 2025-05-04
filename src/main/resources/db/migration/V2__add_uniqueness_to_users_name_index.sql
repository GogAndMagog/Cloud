DROP INDEX users_name;

CREATE UNIQUE INDEX users_name ON users (name);