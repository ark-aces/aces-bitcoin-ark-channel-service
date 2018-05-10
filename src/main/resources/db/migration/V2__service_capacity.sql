CREATE TABLE service_capacities (
  pid BIGSERIAL PRIMARY KEY,
  available_amount DECIMAL(40, 8),
  unsettled_amount DECIMAL(40, 8),
  total_amount DECIMAL(40, 8),
  unit VARCHAR(20),
  updated_at TIMESTAMP,
  created_at TIMESTAMP
);
CREATE INDEX ON service_capacities (available_amount);
CREATE INDEX ON service_capacities (unsettled_amount);
CREATE INDEX ON service_capacities (total_amount);
CREATE INDEX ON service_capacities (unit);
CREATE INDEX ON service_capacities (updated_at);
CREATE INDEX ON service_capacities (created_at);
