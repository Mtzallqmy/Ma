-- Exact NLM identity lookup evidence is in docs/data/rxnorm-identifiers-20261024.json.
DO $rxnorm$ BEGIN
INSERT INTO sources(id,title,url,publisher,retrieved_at,reuse_license,attribution) VALUES ('src-rxnorm-nlm','NLM RxNorm active ingredient identifiers','https://lhncbc.nlm.nih.gov/RxNav/APIs/RxNormAPIs.html','U.S. National Library of Medicine','2026-10-24','RxNorm non-proprietary identifiers; RxNav Terms of Service apply','Exact active IN concept lookup; no clinical approval asserted.') ON CONFLICT DO NOTHING;
INSERT INTO ingredient_identifiers(ingredient_id,vocabulary,identifier,source_id) VALUES ('ing-bisoprolol','RXNORM','19484','src-rxnorm-nlm') ON CONFLICT DO NOTHING;
INSERT INTO citations(entity_type,entity_id,source_id,citation_note) VALUES ('INGREDIENT','ing-bisoprolol','src-rxnorm-nlm','Identity metadata only; scientific review deferred.') ON CONFLICT DO NOTHING;
INSERT INTO ingredient_identifiers(ingredient_id,vocabulary,identifier,source_id) VALUES ('ing-lisinopril','RXNORM','29046','src-rxnorm-nlm') ON CONFLICT DO NOTHING;
INSERT INTO citations(entity_type,entity_id,source_id,citation_note) VALUES ('INGREDIENT','ing-lisinopril','src-rxnorm-nlm','Identity metadata only; scientific review deferred.') ON CONFLICT DO NOTHING;
INSERT INTO ingredient_identifiers(ingredient_id,vocabulary,identifier,source_id) VALUES ('ing-amlodipine','RXNORM','17767','src-rxnorm-nlm') ON CONFLICT DO NOTHING;
INSERT INTO citations(entity_type,entity_id,source_id,citation_note) VALUES ('INGREDIENT','ing-amlodipine','src-rxnorm-nlm','Identity metadata only; scientific review deferred.') ON CONFLICT DO NOTHING;
INSERT INTO ingredient_identifiers(ingredient_id,vocabulary,identifier,source_id) VALUES ('ing-furosemide','RXNORM','4603','src-rxnorm-nlm') ON CONFLICT DO NOTHING;
INSERT INTO citations(entity_type,entity_id,source_id,citation_note) VALUES ('INGREDIENT','ing-furosemide','src-rxnorm-nlm','Identity metadata only; scientific review deferred.') ON CONFLICT DO NOTHING;
INSERT INTO ingredient_identifiers(ingredient_id,vocabulary,identifier,source_id) VALUES ('ing-salbutamol','RXNORM','435','src-rxnorm-nlm') ON CONFLICT DO NOTHING;
INSERT INTO citations(entity_type,entity_id,source_id,citation_note) VALUES ('INGREDIENT','ing-salbutamol','src-rxnorm-nlm','Identity metadata only; scientific review deferred.') ON CONFLICT DO NOTHING;
INSERT INTO ingredient_identifiers(ingredient_id,vocabulary,identifier,source_id) VALUES ('ing-omeprazole','RXNORM','7646','src-rxnorm-nlm') ON CONFLICT DO NOTHING;
INSERT INTO citations(entity_type,entity_id,source_id,citation_note) VALUES ('INGREDIENT','ing-omeprazole','src-rxnorm-nlm','Identity metadata only; scientific review deferred.') ON CONFLICT DO NOTHING;
INSERT INTO ingredient_identifiers(ingredient_id,vocabulary,identifier,source_id) VALUES ('ing-metformin','RXNORM','6809','src-rxnorm-nlm') ON CONFLICT DO NOTHING;
INSERT INTO citations(entity_type,entity_id,source_id,citation_note) VALUES ('INGREDIENT','ing-metformin','src-rxnorm-nlm','Identity metadata only; scientific review deferred.') ON CONFLICT DO NOTHING;
INSERT INTO ingredient_identifiers(ingredient_id,vocabulary,identifier,source_id) VALUES ('ing-dapagliflozin','RXNORM','1488564','src-rxnorm-nlm') ON CONFLICT DO NOTHING;
INSERT INTO citations(entity_type,entity_id,source_id,citation_note) VALUES ('INGREDIENT','ing-dapagliflozin','src-rxnorm-nlm','Identity metadata only; scientific review deferred.') ON CONFLICT DO NOTHING;
END $rxnorm$;
