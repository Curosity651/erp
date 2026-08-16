ALTER TABLE sku
    ADD COLUMN outer_length_mm INT NULL COMMENT '外箱长度，毫米' AFTER package_unit,
    ADD COLUMN outer_width_mm INT NULL COMMENT '外箱宽度，毫米' AFTER outer_length_mm,
    ADD COLUMN outer_height_mm INT NULL COMMENT '外箱高度，毫米' AFTER outer_width_mm,
    ADD COLUMN outer_gross_weight_g INT NULL COMMENT '单箱毛重，克' AFTER outer_height_mm;

UPDATE sku
SET outer_length_mm = CASE UPPER(package_unit)
        WHEN 'MM' THEN ROUND(package_length)
        WHEN 'CM' THEN ROUND(package_length * 10)
        WHEN 'M' THEN ROUND(package_length * 1000)
    END,
    outer_width_mm = CASE UPPER(package_unit)
        WHEN 'MM' THEN ROUND(package_width)
        WHEN 'CM' THEN ROUND(package_width * 10)
        WHEN 'M' THEN ROUND(package_width * 1000)
    END,
    outer_height_mm = CASE UPPER(package_unit)
        WHEN 'MM' THEN ROUND(package_height)
        WHEN 'CM' THEN ROUND(package_height * 10)
        WHEN 'M' THEN ROUND(package_height * 1000)
    END,
    outer_gross_weight_g = CASE UPPER(weight_unit)
        WHEN 'G' THEN ROUND(weight)
        WHEN 'KG' THEN ROUND(weight * 1000)
    END
WHERE outer_length_mm IS NULL
   OR outer_width_mm IS NULL
   OR outer_height_mm IS NULL
   OR outer_gross_weight_g IS NULL;
