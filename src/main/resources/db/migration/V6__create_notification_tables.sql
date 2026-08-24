-- V6__create_notification_tables.sql

CREATE TABLE notification_templates (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    subject VARCHAR(255),
    content TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_template_school_type_channel UNIQUE(school_id, type, channel)
);

CREATE TABLE reminder_rules (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    days_offset INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    template_id UUID REFERENCES notification_templates(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    recipient_user_id UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    student_id UUID REFERENCES students(id) ON DELETE SET NULL,
    recipient_phone VARCHAR(50),
    recipient_email VARCHAR(255),
    type VARCHAR(50) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    title VARCHAR(255),
    message TEXT NOT NULL,
    scheduled_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_notifications_school_status ON notifications(school_id, status);
CREATE INDEX idx_notifications_school_created ON notifications(school_id, created_at);
CREATE INDEX idx_notifications_student ON notifications(student_id);
