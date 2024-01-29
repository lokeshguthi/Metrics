ALTER TABLE testresult ADD COLUMN missing_files BOOLEAN NULL AFTER internal_error;
ALTER TABLE testresult ADD COLUMN illegal_files BOOLEAN NULL AFTER missing_files;
UPDATE testresult SET missing_files = false;
UPDATE testresult SET illegal_files = false;