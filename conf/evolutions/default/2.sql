# --- !Ups

INSERT INTO "icds" ("code", "name")
VALUES
  ('C00', 'Lab havfli o''smasi'),
  ('C01', 'Og''iz bo''shlig''i va yutqini havfli o''smasi'),
  ('C02', 'Milkda havfli o''sma'),
  ('C04', 'Og''iz boshlig''i tubi havfli o''smasi');

# --- !Downs

DELETE FROM "icds";
