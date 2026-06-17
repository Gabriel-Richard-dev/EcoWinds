UPDATE esp_devices SET air_on = false WHERE air_on IS NULL;
ALTER TABLE esp_devices ALTER COLUMN air_on SET NOT NULL;
