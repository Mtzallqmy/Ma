-- Publish byte-verified educational pack repair; no clinical approval.
DO $repair$ BEGIN
INSERT INTO media_assets(id,bucket_name,object_key,content_type,size_bytes,sha256_checksum,access_policy,quality_level,license,attribution,source_id,status_code,public_url)
SELECT 'asset-respiratory-diseases-json-v1-0-1',bucket_name,'stage05/respiratory-diseases-ar-v1/1.0.1/content/cardiac_conduction_ar.json',content_type,3123,'f687af6ec7ef10eadf5e9e9266e545e6dac91ce1dc3149950a640130bf07c5a2',access_policy,quality_level,license,attribution,source_id,status_code,'https://raw.githubusercontent.com/Mtzallqmy/Ma/d7f00c7752e833e5027a8e2b6451838f8d0cc950/app/src/main/assets/packs/respiratory-diseases-ar-v1/content/cardiac_conduction_ar.json' FROM media_assets WHERE id='asset-respiratory-diseases-json-v1' ON CONFLICT DO NOTHING;
UPDATE content_pack_files SET asset_id='asset-respiratory-diseases-json-v1-0-1',size_bytes=3123,sha256_checksum='f687af6ec7ef10eadf5e9e9266e545e6dac91ce1dc3149950a640130bf07c5a2' WHERE pack_id='respiratory-diseases-ar-v1' AND asset_id='asset-respiratory-diseases-json-v1';
UPDATE content_packs SET version='1.0.1',schema_version=1,total_size_bytes=3123 WHERE pack_id='respiratory-diseases-ar-v1' AND version='1.0.0' AND schema_version=2;
UPDATE content_packs SET version='1.0.1',schema_version=1 WHERE pack_id='heart-model-low-v1' AND version='1.0.0' AND schema_version=2;
END $repair$;
