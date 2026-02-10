-- Ensure appointment status column supports new enum values
ALTER TABLE appointments
  MODIFY COLUMN status VARCHAR(32) NOT NULL;
