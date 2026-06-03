ALTER TABLE esp_devices ADD COLUMN api_key_hash VARCHAR(128);
ALTER TABLE esp_devices ADD COLUMN last_heartbeat_at TIMESTAMP;

CREATE INDEX idx_esp_api_key_hash ON esp_devices (api_key_hash);
