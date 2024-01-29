CREATE TABLE users
(
  userid             INT NOT NULL AUTO_INCREMENT (1),
  firstname          VARCHAR(50)  NOT NULL,
  lastname           VARCHAR(50)  NOT NULL,
  username           VARCHAR(50)  NOT NULL UNIQUE,
  studentid          VARCHAR(50)      NULL UNIQUE,
  password           VARCHAR(100)  NOT NULL,
  email              VARCHAR(200) NOT NULL,
  verified           BOOLEAN      NOT NULL DEFAULT FALSE,
  admin              BOOLEAN      NOT NULL DEFAULT FALSE,
  code               VARCHAR(100)      NULL,
  PRIMARY KEY (userid)
);

CREATE TABLE user_rights
(
  id                INT NOT NULL AUTO_INCREMENT,
  userid            INT NOT NULL,
  exerciseid        VARCHAR(50)  NOT NULL,
  role              VARCHAR(50)  NOT NULL,
  groupid           VARCHAR(50)      NULL,
  teamid            VARCHAR(50)      NULL,

  PRIMARY KEY (id),
  FOREIGN KEY (userid) REFERENCES users(userid),
  FOREIGN KEY (exerciseid) REFERENCES exercises(id)
);

CREATE INDEX user_rights_uid ON user_rights(userid);