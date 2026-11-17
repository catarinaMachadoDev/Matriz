DO $$
BEGIN
  IF to_regclass('demo_seed_marker') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM demo_seed_marker WHERE id = 1) THEN
      UPDATE app_user
      SET username = 'catarina', display_name = 'Catarina'
      WHERE username = 'alex' AND display_name = 'Alex Morgan'
        AND NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'catarina');
    END IF;
  END IF;
END;
$$;
