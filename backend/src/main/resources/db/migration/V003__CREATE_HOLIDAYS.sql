CREATE TABLE holidays (
    id BIGSERIAL PRIMARY KEY,
    holiday_date DATE NOT NULL,
    name VARCHAR(255) NOT NULL,
    scope VARCHAR(32) NOT NULL,
    source VARCHAR(64) NOT NULL,
    type VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_holiday_date_scope UNIQUE (holiday_date, scope)
);

CREATE INDEX idx_holidays_date ON holidays (holiday_date);
CREATE INDEX idx_holidays_scope ON holidays (scope);
