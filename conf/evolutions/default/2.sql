# --- !Ups

INSERT INTO "regions" ("id", "name")
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

INSERT INTO "districts" ("id", "name", "region_id")
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

INSERT INTO "villages" ("id", "name", "district_id")
VALUES
  (1, 'Xonqa shaharchasi', 7),
  (2, 'Qirq yop', 7),
  (3, 'Sarapoyon', 7),
  (4, 'Qaramazi', 7);

INSERT INTO "icds" ("code", "name")
VALUES
  ('C00', 'Lab havfli o''smasi'),
  ('C01', 'Og''iz bo''shlig''i va yutqini havfli o''smasi'),
  ('C02', 'Milkda havfli o''sma'),
  ('C04', 'Og''iz boshlig''i tubi havfli o''smasi');

# --- !Downs

DELETE FROM "regions";
DELETE FROM "districts";
DELETE FROM "villages";
DELETE FROM "icds";
