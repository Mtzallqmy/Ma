-- Original anatomical illustration; adds provenance only, no clinical review claims.
-- Source license checked on 2026-10-25: CC BY 3.0, OpenStax College.
DO $migration$
BEGIN
 INSERT INTO sources(id,title,url,publisher,publication_date,retrieved_at,reuse_license,attribution)
 VALUES('src-openstax-heart-plate','Internal Anatomy of the Heart — OpenStax College','https://commons.wikimedia.org/wiki/File:2008_Internal_Anatomy_of_the_HeartN.jpg','OpenStax College','2013','2026-10-25T00:00:00Z','CC BY 3.0 — https://creativecommons.org/licenses/by/3.0/','OpenStax College. Internal Anatomy of the Heart. Original JPEG unchanged; display scaling only.')
 ON CONFLICT(id) DO NOTHING;
 INSERT INTO citations(entity_type,entity_id,source_id,relationship_type,citation_note)
 VALUES('ORGAN','organ-heart','src-openstax-heart-plate','ILLUSTRATION','Original anatomical plate. Independent clinical review not claimed.') ON CONFLICT DO NOTHING;
 INSERT INTO media_assets(id,bucket_name,object_key,content_type,size_bytes,sha256_checksum,access_policy,quality_level,license,attribution,source_id,status_code,public_url)
 VALUES('asset-openstax-heart-plate','github-public-assets','anatomy/openstax-heart-original-v1.jpg','image/jpeg',777730,'5d962a658cab2538378fa846e2276c67389902c51968d64950873b9850938437','public_read','standard','CC BY 3.0 — https://creativecommons.org/licenses/by/3.0/','OpenStax College. Original JPEG unchanged.','src-openstax-heart-plate','EDUCATIONAL_FOUNDATION_PUBLISHED','https://raw.githubusercontent.com/Mtzallqmy/Ma/dfebffd2b923f91041fd3e8a78db00995ca3584a/app/src/main/assets/anatomy/openstax-heart.jpg')
 ON CONFLICT(id) DO NOTHING;
END $migration$;
