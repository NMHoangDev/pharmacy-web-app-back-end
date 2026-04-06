ALTER TABLE appointments
  ADD COLUMN rescheduled_from_id CHAR(36) NULL,
  ADD COLUMN reschedule_reason TEXT NULL,
  ADD COLUMN refund_reason TEXT NULL,
  ADD COLUMN no_show_reason TEXT NULL;

CREATE TABLE IF NOT EXISTS appointment_audit_log (
  id CHAR(36) PRIMARY KEY,
  appointment_id CHAR(36) NOT NULL,
  action VARCHAR(64) NOT NULL,
  before_status VARCHAR(32) NULL,
  after_status VARCHAR(32) NULL,
  reason TEXT NULL,
  actor_id CHAR(36) NULL,
  actor_role VARCHAR(32) NULL,
  actor_ip VARCHAR(64) NULL,
  metadata TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_audit_appointment (appointment_id),
  CONSTRAINT fk_audit_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id)
);

CREATE TABLE IF NOT EXISTS pharmacist_rosters (
  id CHAR(36) PRIMARY KEY,
  pharmacist_id CHAR(36) NOT NULL,
  day_of_week INT NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  INDEX idx_roster_pharmacist_day (pharmacist_id, day_of_week)
);

CREATE TABLE IF NOT EXISTS pharmacist_time_off (
  id CHAR(36) PRIMARY KEY,
  pharmacist_id CHAR(36) NOT NULL,
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL,
  reason TEXT NULL,
  INDEX idx_timeoff_pharmacist (pharmacist_id, start_at, end_at)
);