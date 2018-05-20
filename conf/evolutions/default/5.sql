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
-- Xorazm
  (1, 'Bog''ot', 12),
  (2, 'Gurlan', 12),
  (3, 'Qo''shko''pir', 12),
  (4, 'Hazorasp', 12),
  (5, 'Shovot', 12),
  (6, 'Xiva', 12),
  (7, 'Xonqa', 12),
  (8, 'Yangiariq', 12),
  (9, 'Yangibozor', 12),
  (10, 'Urganch', 12),
-- Andijon
  (11, 'Andijon', 1),
  (12, 'Asaka', 1),
  (13, 'Baliqchi', 1),
  (14, 'Bo''z', 1),
  (15, 'Buloqboshi', 1),
  (16, 'Izboskan', 1),
  (17, 'Jalaquduq', 1),
  (18, 'Marhamat', 1),
  (19, 'Oltinko''l', 1),
  (20, 'Paxtaobod', 1),
  (21, 'Qo''rg''ontepa', 1),
  (22, 'Shahrixon', 1),
  (23, 'Ulug''nor', 1),
  (24, 'Xo''jaobod', 1),
-- Buxoro
  (25, 'Olot', 2),
  (26, 'Buxoro', 2),
  (27, 'G''ijduvon', 2),
  (28, 'Jondor', 2),
  (29, 'Kogon', 2),
  (30, 'Qarako''l', 2),
  (31, 'Qorovulbozor', 2),
  (32, 'Peshku', 2),
  (33, 'Romitan', 2),
  (34, 'Shofirkon', 2),
  (35, 'Vobkent', 2),
-- Farg'ona
  (36, 'Oltiariq', 3),
  (37, 'Bag''dod', 3),
  (38, 'Beshriq', 3),
  (39, 'Buvayda', 3),
  (40, 'Dang''ari', 3),
  (41, 'Farg''ona', 3),
  (42, 'Furqat', 3),
  (43, 'Qo''shtepa', 3),
  (44, 'Quva', 3),
  (45, 'Rishton', 3),
  (46, 'So''x', 3),
  (47, 'Toshloq', 3),
  (48, 'Uchko''prik', 3),
  (49, 'O''zbekiston', 3),
  (50, 'Yozyovon', 3);

INSERT INTO "villages" ("id", "name", "district_id")
VALUES
  (1, 'Xonqa shaharchasi', 7),
  (2, 'Qirqyop', 7),
  (3, 'Sarapoyon', 7),
  (4, 'Qoraqosh', 7),
  (5, 'Amudaryo', 7),
  (6, 'Jirmiz', 7),
  (7, 'Madir', 7),
  (8, 'Navxos', 7),
  (9, 'Namuna', 7),
  (10, 'Olaja', 7),
  (11, 'Tomadurgʻodik', 7);

# --- !Downs

DELETE FROM "regions";
DELETE FROM "districts";
DELETE FROM "villages";
