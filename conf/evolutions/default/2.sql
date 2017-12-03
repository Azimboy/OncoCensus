# --- !Ups

CREATE TABLE "patients" (
  "id" SERIAL PRIMARY KEY,
  "created_at" TIMESTAMP NOT NULL,
  "updated_at"  TIMESTAMP NULL,
  "first_name_encr" VARCHAR NULL,
  "last_name_encr" VARCHAR NULL,
  "middle_name_encr" VARCHAR NULL,
  "gender" INTEGER NULL,
  "birth_date" TIMESTAMP NULL,
  "district_id" INTEGER NOT NULL CONSTRAINT "patients_fk_district_id" REFERENCES "districts" ON UPDATE CASCADE ON DELETE CASCADE,
  "email_encr" VARCHAR NULL,
  "phone_number_encr" VARCHAR NULL,
  "patient_data_json" JSONB NULL
);

# --- !Downs
DROP TABLE "patients";
