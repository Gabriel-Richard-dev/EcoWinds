CREATE TABLE schedule_imports (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(512) NOT NULL,
    file_hash VARCHAR(128) NOT NULL,
    source VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    parser_used VARCHAR(128),
    rooms_affected INTEGER NOT NULL DEFAULT 0,
    schedules_created INTEGER NOT NULL DEFAULT 0,
    schedules_deleted INTEGER NOT NULL DEFAULT 0,
    rooms_created INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_schedule_imports_hash ON schedule_imports (file_hash);
CREATE INDEX idx_schedule_imports_filename ON schedule_imports (filename);
CREATE INDEX idx_schedule_imports_started_at ON schedule_imports (started_at DESC);
