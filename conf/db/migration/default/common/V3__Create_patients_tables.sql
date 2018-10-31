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
  "icd"                 VARCHAR   NOT NULL,
  "client_group"        VARCHAR   NOT NULL,
  "patient_data_json"   JSONB     NULL,
  "supervised_out_json" JSONB     NULL,
  UNIQUE ("passport_id")
);

CREATE TABLE "check_ups" (
  "id"                SERIAL PRIMARY KEY,
  "patient_id"        INTEGER   NOT NULL CONSTRAINT "check_ups_fk_patient_id" REFERENCES "patients" ON UPDATE CASCADE ON DELETE CASCADE,
  "user_id"           INTEGER   NOT NULL CONSTRAINT "check_ups_fk_user_id" REFERENCES "users" ON UPDATE CASCADE ON DELETE CASCADE,
  "created_at"        TIMESTAMP NOT NULL,
  "started_at"        TIMESTAMP NOT NULL,
  "finished_at"       TIMESTAMP NULL,
  "complaint"         VARCHAR   NOT NULL,
  "obj_info"          VARCHAR   NOT NULL,
  "obj_review"        VARCHAR   NOT NULL,
  "status_localis"    VARCHAR   NOT NULL,
  "diagnose"          VARCHAR   NOT NULL,
  "recommendation"    VARCHAR   NOT NULL,
  "receive_info_json" JSONB     NULL
);

CREATE TABLE "check_up_files" (
  "id"                 SERIAL PRIMARY KEY,
  "chech_up_id"        INTEGER   NOT NULL CONSTRAINT "check_up_files_fk_check_up_id" REFERENCES "check_ups" ON UPDATE CASCADE ON DELETE CASCADE,
  "uploaded_at"        TIMESTAMP NOT NULL,
  "file_id"            VARCHAR   NOT NULL,
  "original_file_name" VARCHAR   NOT NULL,
  "obj_review"         VARCHAR   NOT NULL,
  "status_localis"     VARCHAR   NOT NULL,
  "diagnose"           VARCHAR   NOT NULL,
  "recommendation"     VARCHAR   NOT NULL
);