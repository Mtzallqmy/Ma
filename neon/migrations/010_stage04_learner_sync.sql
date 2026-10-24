DO $migration$ BEGIN
CREATE TABLE IF NOT EXISTS academy.device_links(device_hash text PRIMARY KEY,user_code text NOT NULL UNIQUE,expires_at timestamptz NOT NULL,approved_user_id text);
CREATE TABLE IF NOT EXISTS academy.device_sessions(token_hash text PRIMARY KEY,user_id text NOT NULL,expires_at timestamptz NOT NULL);
CREATE TABLE IF NOT EXISTS academy.published_versions(lesson_id text NOT NULL,revision integer NOT NULL,payload jsonb NOT NULL,PRIMARY KEY(lesson_id,revision));
INSERT INTO academy.published_versions SELECT id,published_revision,published_body FROM academy.lessons WHERE published_revision IS NOT NULL ON CONFLICT DO NOTHING;
CREATE OR REPLACE FUNCTION academy.keep_publication() RETURNS trigger LANGUAGE plpgsql SET search_path=pg_catalog,academy AS $$ BEGIN
 IF NEW.published_revision IS NOT NULL THEN INSERT INTO academy.published_versions VALUES(NEW.id,NEW.published_revision,NEW.published_body) ON CONFLICT DO NOTHING; END IF;RETURN NEW;END $$;
IF NOT EXISTS(SELECT 1 FROM pg_trigger WHERE tgname='academy_keep_publication' AND tgrelid='academy.lessons'::regclass) THEN CREATE TRIGGER academy_keep_publication AFTER INSERT OR UPDATE OF published_body ON academy.lessons FOR EACH ROW EXECUTE FUNCTION academy.keep_publication();END IF;
CREATE TABLE IF NOT EXISTS academy.learner_attempts(user_id text NOT NULL,id text NOT NULL,payload jsonb NOT NULL CHECK(octet_length(payload::text)<=16384),received_at timestamptz NOT NULL DEFAULT now(),PRIMARY KEY(user_id,id));
CREATE INDEX IF NOT EXISTS device_links_expiry ON academy.device_links(expires_at);
CREATE INDEX IF NOT EXISTS device_sessions_expiry ON academy.device_sessions(expires_at);
REVOKE ALL ON ALL TABLES IN SCHEMA academy FROM PUBLIC,anonymous;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA academy FROM PUBLIC,anonymous;
END $migration$;
