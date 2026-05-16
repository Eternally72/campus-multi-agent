CREATE TABLE conversation_summaries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    summary LONGTEXT NOT NULL,
    key_points LONGTEXT,
    open_tasks LONGTEXT,
    last_message_id BIGINT,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_summary_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT fk_summary_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
);

CREATE UNIQUE INDEX uk_summary_session ON conversation_summaries(session_id);
CREATE INDEX idx_summary_user ON conversation_summaries(user_id);

CREATE TABLE user_memory_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    memory_key VARCHAR(80) NOT NULL,
    memory_value VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL,
    confidence DOUBLE NOT NULL,
    source VARCHAR(80),
    source_session_id BIGINT,
    replaced_by_id BIGINT,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    forgotten_at TIMESTAMP(6),
    CONSTRAINT fk_pref_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT fk_pref_session FOREIGN KEY (source_session_id) REFERENCES chat_sessions(id),
    CONSTRAINT fk_pref_replaced_by FOREIGN KEY (replaced_by_id) REFERENCES user_memory_preferences(id)
);

CREATE INDEX idx_pref_user_status ON user_memory_preferences(user_id, status);
CREATE INDEX idx_pref_user_key_status ON user_memory_preferences(user_id, memory_key, status);

CREATE TABLE user_memory_facts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category VARCHAR(80) NOT NULL,
    fact_key VARCHAR(120) NOT NULL,
    fact_value VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    confidence DOUBLE NOT NULL,
    source VARCHAR(80),
    source_session_id BIGINT,
    replaced_by_id BIGINT,
    expires_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    forgotten_at TIMESTAMP(6),
    CONSTRAINT fk_fact_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT fk_fact_session FOREIGN KEY (source_session_id) REFERENCES chat_sessions(id),
    CONSTRAINT fk_fact_replaced_by FOREIGN KEY (replaced_by_id) REFERENCES user_memory_facts(id)
);

CREATE INDEX idx_fact_user_status ON user_memory_facts(user_id, status);
CREATE INDEX idx_fact_user_key_status ON user_memory_facts(user_id, fact_key, status);
CREATE INDEX idx_fact_expires ON user_memory_facts(expires_at);
