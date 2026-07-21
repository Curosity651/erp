package com.erp.admin.wms.util;

import com.erp.admin.product.model.entity.Sku;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 体积计算工具类
 * <p>
 * 用于 getSummaryBySku 分页查询时在 Service 层填充体积数据
 */
public final class VolumeCalculator {

    private VolumeCalculator() {
    }

    /**
     * 计算单件体积（m³）
     *
     * @param sku SKU实体
     * @return 体积，缺失尺寸时返回 null
     */
    public static BigDecimal calculateUnitVolume(Sku sku) {
        if (sku == null) {
            return null;
        }
        return calculateUnitVolume(
                sku.getPackageLength(),
                sku.getPackageWidth(),
                sku.getPackageHeight(),
                sku.getPackageUnit()
        );
    }

    /**
     * 计算单件体积（m³）
     *
     * @param length 长度
     * @param width  宽度
     * @param height 高度
     * @param unit   单位（M/CM/MM）
     * @return 体积，缺失尺寸时返回 null
     */
    public static BigDecimal calculateUnitVolume(BigDecimal length, BigDecimal width,
                                                  BigDecimal height, String unit) {
        if (length == null || width == null || height == null) {
            return null;
        }
        if (length.compareTo(BigDecimal.ZERO) <= 0
                || width.compareTo(BigDecimal.ZERO) <= 0
                || height.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal factor = getConversionFactor(unit);
        // volume = length * width * height * factor^3
        return length.multiply(width).multiply(height)
                .multiply(factor.pow(3))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算总体积（m³）
     *
     * @param unitVolume 单件体积
     * @param quantity   数量
     * @return 总体积
     */
    public static BigDecimal calculateTotalVolume(BigDecimal unitVolume, Integer quantity) {
        if (unitVolume == null || quantity == null) {
            return null;
        }
        return unitVolume.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 获取单位转换系数（转为米）
     */
    private static BigDecimal getConversionFactor(String unit) {
        if (unit == null) {
            return BigDecimal.ONE; // 默认为米
        }
        switch (unit.toUpperCase()) {
            case "CM":
                return new BigDecimal("0.01");
            case "MM":
                return new BigDecimal("0.001");
            case "M":
            default:
                return BigDecimal.ONE;
        }
    }
}
