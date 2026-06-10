CREATE TABLE ac_schedules (
    id          BIGSERIAL PRIMARY KEY,
    day_of_week VARCHAR(20)  NOT NULL,
    time        TIME         NOT NULL,
    action      VARCHAR(10)  NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE INDEX idx_ac_schedules_day_enabled ON ac_schedules (day_of_week, enabled);
