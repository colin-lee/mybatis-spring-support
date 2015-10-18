CREATE TABLE IF NOT EXISTS blog (
  id          INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  author      VARCHAR(60)                    NOT NULL,
  content     TEXT                           NOT NULL,
  access_date TIMESTAMP
);

CREATE TABLE IF NOT EXISTS test_page (
  id      INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  `name`  VARCHAR(60)                    NOT NULL UNIQUE,
  `group` VARCHAR                        NOT NULL
);

CREATE TABLE IF NOT EXISTS test_dynamic (
  id          INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  `name`      VARCHAR(60)                    NOT NULL UNIQUE,
  `group`     VARCHAR                        NOT NULL,
  modify_date TIMESTAMP
);
