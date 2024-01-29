ALTER TABLE testresult DROP COLUMN submitted_files;
ALTER TABLE testresult ADD COLUMN snapshot DATETIME AFTER time_request;
UPDATE testresult SET snapshot = time_request WHERE 1;
ALTER TABLE testresult ALTER COLUMN snapshot DATETIME NOT NULL;
ALTER TABLE testresult ADD COLUMN internal_error BOOLEAN NULL AFTER compiled;