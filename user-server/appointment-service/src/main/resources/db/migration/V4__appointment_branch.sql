ALTER TABLE appointments
  ADD COLUMN branch_id CHAR(36) NULL,
  ADD INDEX idx_appointments_branch (branch_id);

ALTER TABLE pharmacist_rosters
  ADD COLUMN branch_id CHAR(36) NULL,
  ADD INDEX idx_roster_branch_pharmacist_day (branch_id, pharmacist_id, day_of_week);

ALTER TABLE pharmacist_time_off
  ADD COLUMN branch_id CHAR(36) NULL,
  ADD INDEX idx_timeoff_branch_pharmacist (branch_id, pharmacist_id, start_at, end_at);
