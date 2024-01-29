/* Adding Loc and comments column in existing table */
ALTER TABLE testresult ADD COLUMN comments_number INT NULL AFTER tests_total;
ALTER TABLE testresult ADD COLUMN loc_number INT NULL AFTER comments_number;
UPDATE testresult SET comments_number = 0;
UPDATE testresult SET loc_number = 0;
