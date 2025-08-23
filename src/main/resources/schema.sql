CREATE EXTENSION IF NOT EXISTS postgis;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(20) DEFAULT 'USER',
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Health profiles table
CREATE TABLE IF NOT EXISTS health_profiles (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    age INTEGER,
    sensitivity_level VARCHAR(20) DEFAULT 'MODERATE',
    has_respiratory_issues BOOLEAN DEFAULT FALSE,
    has_cardiovascular_issues BOOLEAN DEFAULT FALSE,
    is_pregnant BOOLEAN DEFAULT FALSE,
    has_allergies BOOLEAN DEFAULT FALSE,
    preferred_max_aqi INTEGER DEFAULT 100,
    avoid_outbreak_zones BOOLEAN DEFAULT TRUE,
    prefer_green_routes BOOLEAN DEFAULT TRUE,
    UNIQUE(user_id)
);

-- Health conditions table
CREATE TABLE IF NOT EXISTS health_conditions (
    id SERIAL PRIMARY KEY,
    profile_id INTEGER NOT NULL REFERENCES health_profiles(id) ON DELETE CASCADE,
    condition VARCHAR(100) NOT NULL
);

-- Saved routes table
CREATE TABLE IF NOT EXISTS saved_routes (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_point GEOGRAPHY(Point),
    end_point GEOGRAPHY(Point),
    route_data JSONB NOT NULL,
    health_score FLOAT,
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Outbreak reports table (for future use)
CREATE TABLE IF NOT EXISTS outbreak_reports (
    id SERIAL PRIMARY KEY,
    location GEOGRAPHY(Point),
    description TEXT,
    severity INTEGER DEFAULT 1,
    reported_by INTEGER REFERENCES users(id),
    reported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING'
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_health_profiles_user_id ON health_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_saved_routes_user_id ON saved_routes(user_id);
CREATE INDEX IF NOT EXISTS idx_outbreak_reports_location ON outbreak_reports USING GIST(location);