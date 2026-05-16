CREATE TABLE memory_candidates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    session_id BIGINT,
    memory_type VARCHAR(32) NOT NULL,
    memory_key VARCHAR(120) NOT NULL,
    memory_value VARCHAR(1000) NOT NULL,
    category VARCHAR(80),
    confidence DOUBLE NOT NULL,
    reason VARCHAR(500),
    source VARCHAR(80),
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    decided_at TIMESTAMP(6),
    CONSTRAINT fk_candidate_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT fk_candidate_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
);

CREATE INDEX idx_candidate_user_status ON memory_candidates(user_id, status);
CREATE INDEX idx_candidate_session ON memory_candidates(session_id);
