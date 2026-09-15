-- Remove empty one-off backup tables left by the July 2026 data repair.
-- The canonical brand, category, mapping and SKU tables are not affected.
DROP TABLE IF EXISTS bak_jhin_brand_20260713_001;
DROP TABLE IF EXISTS bak_jhin_category_20260713_001;
DROP TABLE IF EXISTS bak_jhin_mapping_20260713_001;
DROP TABLE IF EXISTS bak_jhin_sku_20260713_001;
