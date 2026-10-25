"""Build additive anatomy migration and offline seed from attributed study notes."""
import json
from pathlib import Path
root = Path(__file__).resolve().parents[1]
notes = json.loads((root / 'content/anatomy-foundations-2026.json').read_text())
seed_path = root / 'app/src/main/assets/seed/base_seed_v1.json'
seed = json.loads(seed_path.read_text())
for key in ['organs', 'sources']:
    additions = {row['id']: row for row in notes[key]}
    seed[key] = [row for row in seed[key] if row['id'] not in additions] + list(additions.values())
seed_path.write_text(json.dumps(seed, ensure_ascii=False, indent=2) + '\n')
def literal(value):
    if value is None: return 'NULL'
    if isinstance(value, int): return str(value)
    return "'" + str(value).replace("'", "''") + "'"
statements = []
def insert(table, columns, values):
    statements.append(f"INSERT INTO {table} ({','.join(columns)}) VALUES ({','.join(map(literal, values))}) ON CONFLICT DO NOTHING;")
for source in notes['sources']:
    insert('sources', ['id','title','url','publisher','publication_date','retrieved_at','reuse_license','attribution'],
           [source[key] for key in ['id','title','url','publisher','publicationDate','accessedAt','license','attribution']])
for organ in notes['organs']:
    insert('organs', ['id','system_id','name_ar','name_en','latin_name','summary_ar','summary_en','status_code'],
           [organ[key] for key in ['id','systemId','nameAr','nameEn','latinName','summaryAr','summaryEn']] + ['EDUCATIONAL_FOUNDATION_PUBLISHED'])
    for source in organ['sourceIds']:
        insert('citations', ['entity_type','entity_id','source_id','citation_note'],
               ['ORGAN', organ['id'], source, 'Brief attributed educational adaptation. Independent scientific review deferred; no clinical recommendations.'])
        insert('source_attributions', ['id','entity_id','entity_type','source_id','imported_at','human_reviewed_at','review_status'],
               ['foundation-2026-'+organ['id'],organ['id'],'ORGAN',source,'2026-10-25',None,'under_review'])
    for section in organ['sections']:
        insert('organ_sections', ['id','organ_id','section_key','title_ar','title_en','body_ar','body_en','order_index'],
               [section[key] for key in ['id','organId','sectionKey','titleAr','titleEn','bodyAr','bodyEn','orderIndex']])
(root/'neon/migrations/015_anatomy_foundations.sql').write_text(
    '-- Additive educational notes; no clinical approval, dates or existing records overwritten.\n'
    'DO $anatomy_foundations$\nBEGIN\n' + '\n'.join(statements) + '\nEND $anatomy_foundations$;\n')
print(f'{len(notes["organs"])} new organs; {len(seed["organs"])} total offline organs; {len(statements)} insert-only statements')
