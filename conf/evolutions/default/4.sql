# --- !Ups

CREATE TABLE "check_ups" (
  "id"             SERIAL PRIMARY KEY,
  "patient_id"     INTEGER   NOT NULL CONSTRAINT "check_ups_fk_patient_id" REFERENCES "patients" ON UPDATE CASCADE ON DELETE SET NULL,
  "user_id"        INTEGER   NOT NULL CONSTRAINT "check_ups_fk_user_id" REFERENCES "users" ON UPDATE CASCADE ON DELETE SET NULL,
  "started_at"     TIMESTAMP NOT NULL,
  "finished_at"    TIMESTAMP NULL,
  "complaint"      VARCHAR   NOT NULL,
  "obj_info"       VARCHAR   NOT NULL,
  "obj_review"     VARCHAR   NOT NULL,
  "status_localis" VARCHAR   NOT NULL,
  "diagnose"       VARCHAR   NOT NULL,
  "recommendation" VARCHAR   NOT NULL
);

# --- !Downs

DROP TABLE "check_ups";