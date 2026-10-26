-- Correct two legacy schematic captions; preserve any independently edited titles.
DO $caption_correction$
BEGIN
UPDATE science_figures SET title_ar='مخطط تعليمي: حجرات القلب ومسار التوصيل الكهربائي', title_en='Educational schematic: cardiac chambers and conduction pathway', provenance_type='SCHEMATIC_EDUCATIONAL' WHERE id='fig-cor-conduction-v1' AND title_en='Verified Anatomical Diagram: Four Chambers & Conduction Pathway of the Heart (Cor)' AND provenance_type IN ('SCHEMATIC_EDUCATIONAL','VERIFIED_SCIENTIFIC_DIAGRAM');
UPDATE science_figures SET title_ar='مخطط تعليمي: الشجرة القصبية والوحدة السنخية الشعرية', title_en='Educational schematic: bronchial tree and alveolar-capillary unit', provenance_type='SCHEMATIC_EDUCATIONAL' WHERE id='fig-lungs-alveoli-v1' AND title_en='Verified Diagram: Bronchial Tree & Alveolar-Capillary Unit' AND provenance_type IN ('SCHEMATIC_EDUCATIONAL','VERIFIED_SCIENTIFIC_DIAGRAM');
END $caption_correction$;
