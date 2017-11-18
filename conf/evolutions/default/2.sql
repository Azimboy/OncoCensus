# --- !Ups

CREATE TABLE "regions" (
  "id" SERIAL PRIMARY KEY,
  "name" VARCHAR NOT NULL
);

CREATE TABLE "districts" (
  "id" SERIAL PRIMARY KEY,
  "name" VARCHAR NOT NULL,
  "regionId" INTEGER NOT NULL CONSTRAINT "districtsFkRegionId" REFERENCES "regions" ON UPDATE CASCADE ON DELETE CASCADE
);

INSERT INTO "regions" ("id","name")
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

INSERT INTO "districts" ("id","name", "regionId")
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

# --- !Downs
DROP TABLE "regions";
DROP TABLE "districts";
