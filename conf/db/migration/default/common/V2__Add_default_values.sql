INSERT INTO "regions" ("id", "name")
VALUES
  (1, 'Andijon'),
  (2, 'Buxoro'),
  (3, 'Fargʻona'),
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
  (1, 'Bogʻot', 12),
  (2, 'Gurlan', 12),
  (3, 'Qoʻshkoʻpir', 12),
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
  (14, 'Boʻz', 1),
  (15, 'Buloqboshi', 1),
  (16, 'Izboskan', 1),
  (17, 'Jalaquduq', 1),
  (18, 'Marhamat', 1),
  (19, 'Oltinkoʻl', 1),
  (20, 'Paxtaobod', 1),
  (21, 'Qoʻrgʻontepa', 1),
  (22, 'Shahrixon', 1),
  (23, 'Ulugʻnor', 1),
  (24, 'Xoʻjaobod', 1),
-- Buxoro
  (25, 'Olot', 2),
  (26, 'Buxoro', 2),
  (27, 'Gʻijduvon', 2),
  (28, 'Jondor', 2),
  (29, 'Kogon', 2),
  (30, 'Qarakoʻl', 2),
  (31, 'Qorovulbozor', 2),
  (32, 'Peshku', 2),
  (33, 'Romitan', 2),
  (34, 'Shofirkon', 2),
  (35, 'Vobkent', 2),
-- Fargʻona
  (36, 'Oltiariq', 3),
  (37, 'Bagʻdod', 3),
  (38, 'Beshriq', 3),
  (39, 'Buvayda', 3),
  (40, 'Dangʻari', 3),
  (41, 'Fargʻona', 3),
  (42, 'Furqat', 3),
  (43, 'Qoʻshtepa', 3),
  (44, 'Quva', 3),
  (45, 'Rishton', 3),
  (46, 'Soʻx', 3),
  (47, 'Toshloq', 3),
  (48, 'Uchkoʻprik', 3),
  (49, 'Oʻzbekiston', 3),
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

INSERT INTO "icds" ("code", "name")
VALUES
  ('C00', 'Lab havfli oʻsmasi'),
  ('C01', 'Ogʻiz boʻshligʻi va yutqini havfli oʻsmasi'),
  ('C02', 'Milkda havfli oʻsma'),
  ('C04', 'Ogʻiz boshligʻi tubi havfli oʻsmasi');