# --- !Ups

CREATE TABLE "user_accounts" (
  "id" SERIAL PRIMARY KEY,
  "login_encr" VARCHAR NOT NULL,
  "password_hash_encr" VARCHAR NOT NULL,
  "first_name_encr" VARCHAR NULL,
  "last_name_encr" VARCHAR NULL,
  "role_nodes_encr" VARCHAR NOT NULL,
  "is_deleted" BOOLEAN NOT NULL DEFAULT false,
  "created_at" TIMESTAMP NOT NULL,
  "updated_at"  TIMESTAMP NOT NULL,
  "email_encr" VARCHAR NULL,
  "phone_number_encr" VARCHAR NULL,
  "expires_at" TIMESTAMP NOT NULL,
  "is_first_login" BOOLEAN NOT NULL DEFAULT true,
  "failed_attempts_count" INT NOT NULL DEFAULT 0,
  "blocked_at" TIMESTAMP NULL
);

# --- !Downs
DROP TABLE "UserAccounts";
