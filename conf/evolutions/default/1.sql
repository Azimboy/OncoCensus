# --- !Ups

CREATE TABLE "regions" (
  "id"   SERIAL PRIMARY KEY,
  "name" VARCHAR NOT NULL
);

CREATE TABLE "districts" (
  "id"        SERIAL PRIMARY KEY,
  "name"      VARCHAR NOT NULL,
  "region_id" INTEGER NOT NULL CONSTRAINT "districts_fk_region_id" REFERENCES "regions" ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE "departments" (
  "id"          SERIAL PRIMARY KEY,
  "created_at"  TIMESTAMP NOT NULL,
  "name_encr"   VARCHAR   NOT NULL,
  "district_id" INTEGER   NOT NULL CONSTRAINT "departments_fk_district_id" REFERENCES "districts" ON UPDATE CASCADE ON DELETE CASCADE
);

INSERT INTO
  "regions" ("id", "name")
VALUES
  (1, 'Andijon'),
  (2, 'Buxoro'),
  (3, 'Farg''ona'),
  (4, 'Jizzax'),
  (5, 'Qashqadaryo'),
  (6, 'Namangan'),
  (7, 'Navoiy'),
  (8, 'Toshkent'),
  (9, 'Samarqand'),
  (10, 'Sirdaryo'),
  (11, 'Surxondaryo'),
  (12, 'Xorazm');

INSERT INTO
  "districts" ("id", "name", "region_id")
VALUES
  (1, 'Bog''ot', 12),
  (2, 'Gurlan', 12),
  (3, 'Qo''shko''pir', 12),
  (4, 'Hazorasp', 12),
  (5, 'Shovot', 12),
  (6, 'Xiva', 12),
  (7, 'Xonqa', 12),
  (8, 'Yangiariq', 12),
  (9, 'Yangibozor', 12),
  (10, 'Urganch', 12);

CREATE TABLE "users" (
  "id"                    SERIAL PRIMARY KEY,
  "created_at"            TIMESTAMP NOT NULL,
  "login_encr"            VARCHAR   NOT NULL,
  "password_hash_encr"    VARCHAR   NOT NULL,
  "first_name_encr"       VARCHAR   NULL,
  "last_name_encr"        VARCHAR   NULL,
  "middle_name_encr"      VARCHAR   NULL,
  "department_id"         INTEGER   NOT NULL CONSTRAINT "users_fk_department_id" REFERENCES "departments" ON UPDATE CASCADE ON DELETE SET NULL,
  "role_codes_encr"       VARCHAR   NOT NULL,
  "email_encr"            VARCHAR   NULL,
  "phone_number_encr"     VARCHAR   NULL,
  "updated_at"            TIMESTAMP NULL,
  "expires_at"            TIMESTAMP NULL,
  "failed_attempts_count" INT       NOT NULL DEFAULT 0,
  "blocked_at"            TIMESTAMP NULL
);

# --- !Downs
DROP TABLE "regions";
DROP TABLE "districts";
DROP TABLE "departments";
DROP TABLE "users";
