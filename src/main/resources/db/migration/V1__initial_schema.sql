CREATE TABLE app_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE courses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    term VARCHAR(32),
    teacher VARCHAR(120),
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_courses_owner FOREIGN KEY (owner_id) REFERENCES app_users(id)
);

CREATE TABLE course_materials (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    course_id BIGINT,
    title VARCHAR(180) NOT NULL,
    content LONGTEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_message VARCHAR(500),
    created_at TIMESTAMP(6) NOT NULL,
    indexed_at TIMESTAMP(6),
    CONSTRAINT fk_materials_owner FOREIGN KEY (owner_id) REFERENCES app_users(id),
    CONSTRAINT fk_materials_course FOREIGN KEY (course_id) REFERENCES courses(id)
);

CREATE TABLE todo_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(1000),
    due_date DATE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_todo_owner FOREIGN KEY (owner_id) REFERENCES app_users(id)
);

CREATE TABLE chat_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_sessions_owner FOREIGN KEY (owner_id) REFERENCES app_users(id)
);

CREATE TABLE chat_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    agent_type VARCHAR(32) NOT NULL,
    content LONGTEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_messages_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
);

CREATE INDEX idx_courses_owner ON courses(owner_id);
CREATE INDEX idx_materials_owner ON course_materials(owner_id);
CREATE INDEX idx_todo_owner ON todo_items(owner_id);
CREATE INDEX idx_sessions_owner ON chat_sessions(owner_id);
CREATE INDEX idx_messages_session ON chat_messages(session_id);
