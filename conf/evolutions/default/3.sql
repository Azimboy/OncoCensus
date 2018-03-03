# --- !Ups

INSERT INTO
  "client_groups" ("id", "name", "code")
VALUES
  (1, 'Lab havfli o''smasi', 'C00'),
  (2, 'Og''iz bo''shlig''i va yutqini havfli o''smasi', 'C01'),
  (3, 'Milkda havfli o''sma', 'C03'),
  (4, 'Og''iz boshlig''i tubi havfli o''smasi', 'C04');

# --- !Downs

DELETE FROM "client_groups" WHERE "id" > 0
