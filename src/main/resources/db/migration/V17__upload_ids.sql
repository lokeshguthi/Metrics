ALTER TABLE uploads DROP PRIMARY KEY;
ALTER TABLE uploads ADD id INT NOT NULL AUTO_INCREMENT FIRST;
ALTER TABLE annotations DROP PRIMARY KEY;
ALTER TABLE annotations ADD fileid INT NOT NULL DEFAULT 0 FIRST;
UPDATE annotations a  SET a.fileid = ISNULL(SELECT u.id FROM uploads u
  WHERE
    a.exercise = u.exercise
    AND a.sheet = u.sheet
    AND a.assignment = u.assignment
    AND a.team = u.team
    AND a.filename = CONCAT(FORMATDATETIME(u.upload_date, 'yyyyMMddHHmmss'), '-', u.filename), 0);


CREATE TABLE illegal_annotations AS SELECT * FROM annotations a WHERE a.fileid = 0;
DELETE FROM annotations WHERE fileid = 0;
CREATE TABLE tmp_annotations AS SELECT * FROM annotations;
ALTER TABLE tmp_annotations DROP COLUMN exercise, sheet, assignment, team, filename;
DROP TABLE annotations;
CREATE TABLE annotations AS SELECT * FROM tmp_annotations;
DROP TABLE tmp_annotations;

ALTER TABLE annotations ALTER COLUMN fileid SET NOT NULL;
ALTER TABLE annotations ALTER COLUMN line SET NOT NULL;
ALTER TABLE annotations ADD PRIMARY KEY (fileid, line);
ALTER TABLE annotations ADD FOREIGN KEY (fileid) REFERENCES uploads(id);