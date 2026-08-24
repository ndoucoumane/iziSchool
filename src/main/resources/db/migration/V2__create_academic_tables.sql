-- V2__create_academic_tables.sql

CREATE TABLE academic_years (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_academic_year_school_name UNIQUE(school_id, name)
);

CREATE TABLE grade_levels (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    display_order INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_grade_level_school_code UNIQUE(school_id, code)
);

CREATE TABLE school_classes (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    grade_level_id UUID NOT NULL REFERENCES grade_levels(id) ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    capacity INT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_school_class_year_code UNIQUE(school_id, academic_year_id, code)
);

CREATE INDEX idx_academic_years_school_status ON academic_years(school_id, status);
CREATE INDEX idx_grade_levels_school_order ON grade_levels(school_id, display_order);
CREATE INDEX idx_school_classes_school_year ON school_classes(school_id, academic_year_id);
CREATE INDEX idx_school_classes_grade ON school_classes(grade_level_id);
