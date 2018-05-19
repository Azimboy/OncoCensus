# --- !Ups

CREATE TABLE "patients" (
  "id"                  SERIAL PRIMARY KEY,
  "created_at"          TIMESTAMP NOT NULL,
  "deleted_at"          TIMESTAMP NULL,
  "first_name_encr"     VARCHAR   NULL,
  "last_name_encr"      VARCHAR   NULL,
  "middle_name_encr"    VARCHAR   NULL,
  "passport_id"         VARCHAR   NOT NULL,
  "gender"              INTEGER   NOT NULL,
  "birth_date"          TIMESTAMP NOT NULL,
  "village_id"          INTEGER   NOT NULL CONSTRAINT "patients_fk_village_id" REFERENCES "villages" ON UPDATE CASCADE ON DELETE SET NULL,
  "avatar_id"           VARCHAR   NULL,
  "icd_id"              INTEGER   NOT NULL CONSTRAINT "patients_fk_icd_id" REFERENCES "icds" ON UPDATE CASCADE ON DELETE SET NULL,
  "client_group"        VARCHAR   NOT NULL,
  "patient_data_json"   JSONB     NULL,
  "supervised_out_json" JSONB     NULL,
  UNIQUE ("passport_id")
);

# --- !Downs

DROP TABLE "patients";
