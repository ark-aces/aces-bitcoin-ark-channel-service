CREATE TABLE service_capacity (
  pid BIGSERIAL PRIMARY KEY,
  available_amount DECIMAL(40, 8),
  unsettled_amount DECIMAL(40, 8),
  total_amount DECIMAL(40, 8),
  unit VARCHAR(20),
  updated_at TIMESTAMP,
  created_at TIMESTAMP
);
CREATE INDEX ON service_capacity (available_amount);
CREATE INDEX ON service_capacity (unsettled_amount);
CREATE INDEX ON service_capacity (total_amount);
CREATE INDEX ON service_capacity (unit);
CREATE INDEX ON service_capacity (updated_at);
CREATE INDEX ON service_capacity (created_at);
