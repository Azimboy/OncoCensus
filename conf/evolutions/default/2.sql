# --- !Ups

CREATE TABLE "client_groups" (
  "id" SERIAL PRIMARY KEY,
  "name" VARCHAR NOT NULL,
  "code" VARCHAR NOT NULL
);

CREATE TABLE "patients" (
  "id" SERIAL PRIMARY KEY,
  "created_at" TIMESTAMP NOT NULL,
  "deleted_at"  TIMESTAMP NULL,
  "first_name_encr" VARCHAR NULL,
  "last_name_encr" VARCHAR NULL,
  "middle_name_encr" VARCHAR NULL,
  "gender" INTEGER NULL,
  "birth_date" TIMESTAMP NULL,
  "district_id" INTEGER NOT NULL CONSTRAINT "patients_fk_district_id" REFERENCES "districts" ON UPDATE CASCADE ON DELETE SET NULL,
  "email_encr" VARCHAR NULL,
  "phone_number_encr" VARCHAR NULL,
  "avatar_id" VARCHAR NULL,
  "patient_data_json" JSONB NULL,
  "client_group_id" INTEGER CONSTRAINT "patients_fk_client_group_id" REFERENCES "client_groups" ON UPDATE CASCADE ON DELETE SET NULL,
  "dead_at" TIMESTAMP NULL,
  "dead_reason" VARCHAR NULL
);

# --- !Downs

DROP TABLE "client_groups";
DROP TABLE "patients";
