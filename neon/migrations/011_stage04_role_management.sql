DO $migration$ BEGIN
CREATE OR REPLACE FUNCTION academy.set_membership(p_actor text,p_target text,p_role text) RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,academy AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(hashtext('academy.membership.change'));
 IF NOT EXISTS(SELECT 1 FROM academy.memberships WHERE user_id=p_actor AND role='OWNER') THEN RAISE EXCEPTION 'Owner required' USING ERRCODE='42501'; END IF;
 IF p_role NOT IN ('OWNER','REVIEWER','EDITOR','READ_ONLY','NONE') OR p_role IS NULL THEN RAISE EXCEPTION 'Invalid role' USING ERRCODE='22023'; END IF;
 IF NOT EXISTS(SELECT 1 FROM neon_auth."user" WHERE id::text=p_target) THEN RAISE EXCEPTION 'Unknown identity' USING ERRCODE='P0002'; END IF;
 IF p_role<>'OWNER' AND EXISTS(SELECT 1 FROM academy.memberships WHERE user_id=p_target AND role='OWNER') AND (SELECT count(*) FROM academy.memberships WHERE role='OWNER')<=1 THEN RAISE EXCEPTION 'Cannot remove last owner' USING ERRCODE='42501'; END IF;
 IF p_role='NONE' THEN DELETE FROM academy.memberships WHERE user_id=p_target;
 ELSE INSERT INTO academy.memberships(user_id,role) VALUES(p_target,p_role) ON CONFLICT(user_id) DO UPDATE SET role=EXCLUDED.role; END IF;
 INSERT INTO academy.audit_logs(actor_id,action,entity_id) VALUES(p_actor,'ROLE_'||p_role,p_target);
 RETURN jsonb_build_object('userId',p_target,'role',p_role);
END $$;
REVOKE ALL ON FUNCTION academy.set_membership(text,text,text) FROM PUBLIC,anonymous;
END $migration$;
