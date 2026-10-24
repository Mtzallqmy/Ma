DO $migration$ BEGIN
-- Additive academy storage; no changes to stages 01-03. Reapply safely.
CREATE SCHEMA IF NOT EXISTS academy;
REVOKE ALL ON SCHEMA academy FROM PUBLIC, anonymous;
CREATE TABLE IF NOT EXISTS academy.courses (id text PRIMARY KEY, payload jsonb NOT NULL, is_public boolean NOT NULL DEFAULT false);
CREATE TABLE IF NOT EXISTS academy.modules (id text PRIMARY KEY, course_id text NOT NULL REFERENCES academy.courses(id));
CREATE TABLE IF NOT EXISTS academy.sources (id text PRIMARY KEY, payload jsonb NOT NULL);
CREATE TABLE IF NOT EXISTS academy.memberships (user_id text PRIMARY KEY, role text NOT NULL CHECK(role IN ('OWNER','REVIEWER','EDITOR','READ_ONLY')));
CREATE TABLE IF NOT EXISTS academy.lessons (
 id text PRIMARY KEY, module_id text NOT NULL REFERENCES academy.modules(id), revision integer NOT NULL CHECK(revision>0),
 draft jsonb NOT NULL CHECK(octet_length(draft::text)<=131072), author_id text NOT NULL,
 reviewed_revision integer, published_revision integer, published_body jsonb,
 updated_at timestamptz NOT NULL DEFAULT now(),
 CHECK((published_revision IS NULL)=(published_body IS NULL))
);
CREATE TABLE IF NOT EXISTS academy.versions (lesson_id text NOT NULL REFERENCES academy.lessons(id), revision integer NOT NULL, payload jsonb NOT NULL, PRIMARY KEY(lesson_id,revision));
CREATE TABLE IF NOT EXISTS academy.content_reviews (id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, lesson_id text NOT NULL REFERENCES academy.lessons(id), revision integer NOT NULL, reviewer_id text NOT NULL, note text NOT NULL, reviewed_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE IF NOT EXISTS academy.audit_logs (id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, actor_id text NOT NULL, action text NOT NULL, entity_id text NOT NULL, revision integer, occurred_at timestamptz NOT NULL DEFAULT now());
CREATE OR REPLACE FUNCTION academy.append_only() RETURNS trigger LANGUAGE plpgsql SET search_path=pg_catalog AS $$ BEGIN RAISE EXCEPTION 'Append-only audit record' USING ERRCODE='42501'; END $$;
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_trigger WHERE tgrelid='academy.audit_logs'::regclass AND tgname='academy_audit_immutable') THEN
 CREATE TRIGGER academy_audit_immutable BEFORE UPDATE OR DELETE ON academy.audit_logs FOR EACH ROW EXECUTE FUNCTION academy.append_only();
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_trigger WHERE tgrelid='academy.content_reviews'::regclass AND tgname='academy_reviews_immutable') THEN
 CREATE TRIGGER academy_reviews_immutable BEFORE UPDATE OR DELETE ON academy.content_reviews FOR EACH ROW EXECUTE FUNCTION academy.append_only();
 END IF;
END $$;
CREATE OR REPLACE FUNCTION academy.transition(p_actor text,p_id text,p_expected integer,p_action text,p_body jsonb DEFAULT NULL,p_note text DEFAULT '') RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,academy AS $$
DECLARE member_role text; item academy.lessons%ROWTYPE; next_revision integer;
BEGIN
 SELECT role INTO member_role FROM academy.memberships WHERE user_id=p_actor;
 IF member_role IS NULL OR member_role='READ_ONLY' THEN RAISE EXCEPTION 'Forbidden' USING ERRCODE='42501'; END IF;
 SELECT * INTO item FROM academy.lessons WHERE id=p_id FOR UPDATE;
 IF NOT FOUND THEN RAISE EXCEPTION 'Lesson not found' USING ERRCODE='P0002'; END IF;
 IF item.revision<>p_expected THEN RAISE EXCEPTION 'Revision conflict' USING ERRCODE='40001'; END IF;
 next_revision:=item.revision;
 IF p_action='EDIT' THEN
  IF member_role NOT IN ('EDITOR','OWNER') THEN RAISE EXCEPTION 'Forbidden' USING ERRCODE='42501'; END IF;
  IF p_body IS NULL OR octet_length(p_body::text)>131072 OR p_body->>'id' IS DISTINCT FROM p_id OR p_body->>'moduleId' IS DISTINCT FROM item.module_id
     OR p_body->>'courseId' IS DISTINCT FROM (SELECT course_id FROM academy.modules WHERE id=item.module_id)
     OR jsonb_typeof(p_body->'sourceIds') IS DISTINCT FROM 'array' OR jsonb_array_length(p_body->'sourceIds')=0 THEN
   RAISE EXCEPTION 'Invalid lesson identity or sources' USING ERRCODE='22023';
  END IF;
  IF EXISTS(SELECT 1 FROM jsonb_array_elements_text(p_body->'sourceIds') s WHERE NOT EXISTS(SELECT 1 FROM academy.sources WHERE id=s.value)) THEN
   RAISE EXCEPTION 'Unknown source' USING ERRCODE='22023';
  END IF;
  INSERT INTO academy.versions VALUES(item.id,item.revision,item.draft) ON CONFLICT DO NOTHING;
  next_revision:=item.revision+1;
  UPDATE academy.lessons SET revision=next_revision,draft=p_body||jsonb_build_object('revision',next_revision,'reviewStatus','EDUCATIONAL_UNREVIEWED'),author_id=p_actor,reviewed_revision=NULL,updated_at=now() WHERE id=p_id;
 ELSIF p_action='REVIEW' THEN
  IF member_role NOT IN ('REVIEWER','OWNER') OR item.author_id=p_actor THEN RAISE EXCEPTION 'Independent reviewer required' USING ERRCODE='42501'; END IF;
  IF btrim(p_note)='' THEN RAISE EXCEPTION 'Review note required' USING ERRCODE='22023'; END IF;
  INSERT INTO academy.content_reviews(lesson_id,revision,reviewer_id,note) VALUES(p_id,item.revision,p_actor,left(p_note,4000));
  UPDATE academy.lessons SET reviewed_revision=revision,updated_at=now() WHERE id=p_id;
 ELSIF p_action='PUBLISH' THEN
  IF member_role<>'OWNER' THEN RAISE EXCEPTION 'Owner required' USING ERRCODE='42501'; END IF;
  IF item.reviewed_revision IS DISTINCT FROM item.revision OR NOT EXISTS(SELECT 1 FROM academy.content_reviews WHERE lesson_id=p_id AND revision=item.revision AND reviewer_id<>item.author_id) THEN RAISE EXCEPTION 'Unreviewed revision' USING ERRCODE='42501'; END IF;
  UPDATE academy.lessons SET published_revision=revision,published_body=draft||jsonb_build_object('reviewStatus','HUMAN_REVIEWED_EDUCATIONAL'),updated_at=now() WHERE id=p_id;
 ELSIF p_action='WITHDRAW' THEN
  IF member_role<>'OWNER' THEN RAISE EXCEPTION 'Owner required' USING ERRCODE='42501'; END IF;
  UPDATE academy.lessons SET published_revision=NULL,published_body=NULL,updated_at=now() WHERE id=p_id;
 ELSE RAISE EXCEPTION 'Unknown action' USING ERRCODE='22023';
 END IF;
 INSERT INTO academy.audit_logs(actor_id,action,entity_id,revision) VALUES(p_actor,p_action,p_id,next_revision);
 RETURN jsonb_build_object('id',p_id,'revision',next_revision,'action',p_action);
END $$;
REVOKE ALL ON ALL TABLES IN SCHEMA academy FROM PUBLIC,anonymous;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA academy FROM PUBLIC,anonymous;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA academy FROM PUBLIC,anonymous;

END $migration$;
