-- V1__create_school_tables.sql

CREATE TABLE schools (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255),
    phone VARCHAR(50),
    address VARCHAR(255),
    city VARCHAR(100),
    country VARCHAR(100),
    logo_url VARCHAR(500),
    currency VARCHAR(10) NOT NULL DEFAULT 'XOF',
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0
);

CREATE TABLE user_profiles (
    id UUID PRIMARY KEY,
    keycloak_user_id VARCHAR(100) NOT NULL UNIQUE,
    school_id UUID REFERENCES schools(id) ON DELETE SET NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    role VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_schools_code ON schools(code);
CREATE INDEX idx_schools_status ON schools(status);
CREATE INDEX idx_user_profiles_keycloak_id ON user_profiles(keycloak_user_id);
CREATE INDEX idx_user_profiles_school_id ON user_profiles(school_id);
CREATE INDEX idx_user_profiles_email ON user_profiles(email);
CREATE INDEX idx_user_profiles_school_deleted ON user_profiles(school_id, deleted);
