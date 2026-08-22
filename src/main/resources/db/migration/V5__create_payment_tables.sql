-- V5__create_payment_tables.sql

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    parent_id UUID REFERENCES parents(id) ON DELETE SET NULL,
    payment_reference VARCHAR(100) NOT NULL,
    provider_transaction_id VARCHAR(150),
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    provider VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    payment_date TIMESTAMPTZ NOT NULL,
    description VARCHAR(500),
    metadata TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_payment_reference UNIQUE(payment_reference),
    CONSTRAINT uk_payment_provider_tx UNIQUE(provider, provider_transaction_id),
    CONSTRAINT chk_payment_amount_pos CHECK (amount > 0)
);

CREATE TABLE payment_allocations (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    payment_schedule_id UUID NOT NULL REFERENCES payment_schedules(id) ON DELETE RESTRICT,
    amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_allocation_amount_pos CHECK (amount > 0)
);

CREATE TABLE receipts (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE RESTRICT,
    receipt_number VARCHAR(100) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    pdf_url VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_receipt_payment_id UNIQUE(payment_id),
    CONSTRAINT uk_receipt_school_number UNIQUE(school_id, receipt_number),
    CONSTRAINT chk_receipt_amount_pos CHECK (amount > 0)
);

CREATE INDEX idx_payments_school_date ON payments(school_id, payment_date);
CREATE INDEX idx_payments_school_status ON payments(school_id, status);
CREATE INDEX idx_payments_student ON payments(student_id);
CREATE INDEX idx_payments_reference ON payments(payment_reference);
CREATE INDEX idx_payments_provider_tx ON payments(provider, provider_transaction_id);
CREATE INDEX idx_payment_allocations_payment ON payment_allocations(payment_id);
CREATE INDEX idx_payment_allocations_schedule ON payment_allocations(payment_schedule_id);
CREATE INDEX idx_receipts_school_number ON receipts(school_id, receipt_number);
