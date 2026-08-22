-- V7__create_audit_tables.sql

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    user_id VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_audit_logs_school_created ON audit_logs(school_id, created_at DESC);
CREATE INDEX idx_audit_logs_school_entity ON audit_logs(school_id, entity_type, entity_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
