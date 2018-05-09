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

CREATE TABLE "villages" (
  "id"          SERIAL PRIMARY KEY,
  "name"        VARCHAR NOT NULL,
  "district_id" INTEGER NOT NULL CONSTRAINT "villages_fk_district_id" REFERENCES "districts" ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE "departments" (
  "id"          SERIAL PRIMARY KEY,
  "created_at"  TIMESTAMP NOT NULL,
  "name_encr"   VARCHAR   NOT NULL,
  "district_id" INTEGER   NOT NULL CONSTRAINT "departments_fk_district_id" REFERENCES "districts" ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE "users" (
  "id"                    SERIAL PRIMARY KEY,
  "created_at"            TIMESTAMP NOT NULL,
  "login_encr"            VARCHAR   NOT NULL,
  "password_hash_encr"    VARCHAR   NOT NULL,
  "first_name_encr"       VARCHAR   NULL,
  "last_name_encr"        VARCHAR   NULL,
  "middle_name_encr"      VARCHAR   NULL,
  "department_id"         INTEGER   CONSTRAINT "users_fk_department_id" REFERENCES "departments" ON UPDATE CASCADE ON DELETE SET NULL,
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
DROP TABLE "villages";
DROP TABLE "departments";
DROP TABLE "users";
