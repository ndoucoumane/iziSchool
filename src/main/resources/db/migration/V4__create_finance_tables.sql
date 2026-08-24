-- V4__create_finance_tables.sql

CREATE TABLE fees (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    fee_type VARCHAR(30) NOT NULL,
    mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_fee_school_year_code UNIQUE(school_id, academic_year_id, code),
    CONSTRAINT chk_fee_amount_positive CHECK (amount > 0)
);

CREATE TABLE fee_assignments (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    fee_id UUID NOT NULL REFERENCES fees(id) ON DELETE CASCADE,
    grade_level_id UUID REFERENCES grade_levels(id) ON DELETE CASCADE,
    school_class_id UUID REFERENCES school_classes(id) ON DELETE CASCADE,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_fee_assignment_amount_positive CHECK (amount > 0)
);

CREATE TABLE payment_plans (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    number_of_installments INT NOT NULL,
    frequency VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_payment_plan_installments CHECK (number_of_installments >= 1)
);

CREATE TABLE payment_schedules (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    enrollment_id UUID NOT NULL REFERENCES student_enrollments(id) ON DELETE RESTRICT,
    fee_id UUID NOT NULL REFERENCES fees(id) ON DELETE RESTRICT,
    due_date DATE NOT NULL,
    amount_due NUMERIC(12, 2) NOT NULL,
    amount_paid NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    remaining_amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL,
    installment_number INT NOT NULL,
    description VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_schedule_amount_due_pos CHECK (amount_due > 0),
    CONSTRAINT chk_schedule_amount_paid_nonneg CHECK (amount_paid >= 0),
    CONSTRAINT chk_schedule_remaining_nonneg CHECK (remaining_amount >= 0)
);

CREATE INDEX idx_fees_school_year ON fees(school_id, academic_year_id);
CREATE INDEX idx_fee_assignments_lookup ON fee_assignments(school_id, fee_id, academic_year_id);
CREATE INDEX idx_payment_plans_school_year ON payment_plans(school_id, academic_year_id);
CREATE INDEX idx_payment_schedules_school_student ON payment_schedules(school_id, student_id);
CREATE INDEX idx_payment_schedules_school_due ON payment_schedules(school_id, due_date);
CREATE INDEX idx_payment_schedules_school_status ON payment_schedules(school_id, status);
CREATE INDEX idx_payment_schedules_enrollment ON payment_schedules(enrollment_id);
