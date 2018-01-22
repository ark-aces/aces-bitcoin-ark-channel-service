CREATE TABLE contracts (
  pid BIGSERIAL PRIMARY KEY,
  id VARCHAR(255) NOT NULL,
  correlation_id VARCHAR(255),
  status VARCHAR(20),
  recipient_ark_address VARCHAR(255),
  deposit_btc_address VARCHAR(255),
  subscription_id VARCHAR(255),
  created_at TIMESTAMP
);

CREATE TABLE transfers (
  pid BIGSERIAL PRIMARY KEY,
  id VARCHAR(255) NOT NULL,
  created_at TIMESTAMP,
  contract_pid BIGINT NOT NULL,
  status VARCHAR(255),
  btc_transaction_id VARCHAR(255),
  btc_amount DECIMAL(8,5),
  btc_to_ark_rate DECIMAL(8,5),
  btc_flat_fee DECIMAL(8,5),
  btc_percent_fee DECIMAL(8,5),
  btc_total_fee DECIMAL(8,5),
  ark_send_amount DECIMAL(8,5),
  ark_transaction_id VARCHAR(255),
  needs_ark_confirmation BOOLEAN,
  ark_confirmation_subscription_id VARCHAR(255),
  needs_btc_return BOOLEAN,
  return_btc_transaction_id VARHCHAR(255)
);
ALTER TABLE transfers ADD CONSTRAINT FOREIGN KEY (contract_pid) REFERENCES contracts (pid);
