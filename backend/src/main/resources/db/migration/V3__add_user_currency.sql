ALTER TABLE users
ADD COLUMN currency VARCHAR(3);

ALTER TABLE users
ALTER COLUMN currency SET NOT NULL;

ALTER TABLE users
ADD CONSTRAINT users_currency_check
CHECK (currency IN ('RON', 'EUR', 'USD', 'GBP', 'CAD', 'AUD', 'CHF', 'JPY', 'PLN', 'BGN', 'HUF'));