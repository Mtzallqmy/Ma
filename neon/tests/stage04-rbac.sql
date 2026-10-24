-- Real PostgreSQL permission test; every synthetic row is rolled back.
DO $test$
DECLARE body jsonb; result jsonb; fixture text:='__academy_rbac_fixture__';
BEGIN
 BEGIN
  ASSERT NOT has_schema_privilege('anonymous','academy','USAGE');
  ASSERT NOT has_function_privilege('anonymous','academy.transition(text,text,integer,text,jsonb,text)','EXECUTE');
  BEGIN SET LOCAL ROLE anonymous; PERFORM 1 FROM academy.lessons; RAISE EXCEPTION 'Anonymous read drafts'; EXCEPTION WHEN insufficient_privilege THEN NULL; END;
  INSERT INTO academy.memberships VALUES('__test_editor__','EDITOR'),('__test_reviewer__','REVIEWER'),('__test_owner__','OWNER'),('__test_reader__','READ_ONLY');
  SELECT draft||jsonb_build_object('id',fixture) INTO body FROM academy.lessons WHERE id='academy-units';
  ASSERT body IS NOT NULL;
  INSERT INTO academy.lessons(id,module_id,revision,draft,author_id) VALUES(fixture,'module-units',1,body,'__test_editor__');
  BEGIN PERFORM academy.transition('__test_reader__',fixture,1,'EDIT',body); RAISE EXCEPTION 'ReadOnly escalated'; EXCEPTION WHEN insufficient_privilege THEN NULL; END;
  BEGIN PERFORM academy.transition('__unknown__',fixture,1,'EDIT',body); RAISE EXCEPTION 'Unknown escalated'; EXCEPTION WHEN insufficient_privilege THEN NULL; END;
  BEGIN PERFORM academy.transition('__test_owner__',fixture,1,'PUBLISH'); RAISE EXCEPTION 'Unreviewed published'; EXCEPTION WHEN insufficient_privilege THEN NULL; END;
  result:=academy.transition('__test_editor__',fixture,1,'EDIT',body);
  ASSERT (result->>'revision')::int=2;
  BEGIN PERFORM academy.transition('__test_editor__',fixture,2,'REVIEW',NULL,'self approval'); RAISE EXCEPTION 'Self approval'; EXCEPTION WHEN insufficient_privilege THEN NULL; END;
  PERFORM academy.transition('__test_reviewer__',fixture,2,'REVIEW',NULL,'Synthetic permission test only');
  BEGIN PERFORM academy.transition('__test_editor__',fixture,2,'PUBLISH'); RAISE EXCEPTION 'Editor published'; EXCEPTION WHEN insufficient_privilege THEN NULL; END;
  PERFORM academy.transition('__test_owner__',fixture,2,'PUBLISH');
  ASSERT (SELECT published_revision FROM academy.lessons WHERE id=fixture)=2;
  PERFORM academy.transition('__test_editor__',fixture,2,'EDIT',body);
  ASSERT (SELECT published_revision FROM academy.lessons WHERE id=fixture)=2;
  BEGIN PERFORM academy.transition('__test_owner__',fixture,3,'PUBLISH'); RAISE EXCEPTION 'Stale review reused'; EXCEPTION WHEN insufficient_privilege THEN NULL; END;
  BEGIN PERFORM academy.transition('__test_editor__',fixture,1,'EDIT',body); RAISE EXCEPTION 'Conflict lost'; EXCEPTION WHEN serialization_failure THEN NULL; END;
  PERFORM academy.transition('__test_owner__',fixture,3,'WITHDRAW');
  ASSERT (SELECT published_body IS NULL FROM academy.lessons WHERE id=fixture);
  BEGIN UPDATE academy.audit_logs SET action='tampered' WHERE entity_id=fixture; RAISE EXCEPTION 'Audit mutable'; EXCEPTION WHEN insufficient_privilege THEN NULL; END;
  RAISE EXCEPTION 'Rollback synthetic fixtures' USING ERRCODE='Z0001';
 EXCEPTION WHEN SQLSTATE 'Z0001' THEN NULL;
 END;
 RAISE NOTICE 'Stage04 RBAC: all assertions passed; synthetic fixtures rolled back';
END $test$;
