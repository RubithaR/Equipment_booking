-- Phase 1 baseline schema for user-service.
-- Project intentionally starts fresh — any pre-existing tables in this schema
-- are dropped here. Flyway records this migration in flyway_schema_history,
-- so it only runs once.

DROP TABLE IF EXISTS departments CASCADE;
DROP TABLE IF EXISTS faculties   CASCADE;
DROP TABLE IF EXISTS users       CASCADE;

CREATE TABLE faculties (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(200) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE departments (
    id           BIGSERIAL PRIMARY KEY,
    faculty_id   BIGINT       NOT NULL REFERENCES faculties(id),
    code         VARCHAR(20)  NOT NULL,
    name         VARCHAR(200) NOT NULL,
    hod_user_id  BIGINT,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (faculty_id, code)
);

CREATE TABLE users (
    id                 BIGSERIAL PRIMARY KEY,
    email              VARCHAR(200) NOT NULL UNIQUE,
    password           VARCHAR(200) NOT NULL,
    full_name          VARCHAR(200) NOT NULL,
    role               VARCHAR(40)  NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    faculty_id         BIGINT       REFERENCES faculties(id),
    department_id     BIGINT       REFERENCES departments(id),
    phone_number       VARCHAR(20),
    en_number          VARCHAR(20)  UNIQUE,
    index_number       VARCHAR(20)  UNIQUE,
    name_with_initial  VARCHAR(200),
    uni_email          VARCHAR(200) UNIQUE,
    id_photo_url       VARCHAR(500),
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_role        ON users(role);
CREATE INDEX idx_users_faculty     ON users(faculty_id);
CREATE INDEX idx_users_department  ON users(department_id);
CREATE INDEX idx_users_role_status ON users(role, status);

ALTER TABLE departments
    ADD CONSTRAINT fk_departments_hod
    FOREIGN KEY (hod_user_id) REFERENCES users(id);
