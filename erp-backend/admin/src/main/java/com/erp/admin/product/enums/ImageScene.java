package com.erp.admin.product.enums;

/**
 * 图片使用场景枚举
 * <p>
 * 用于区分不同场景下图片 URL 的生成策略
 *
 * @author system
 */
public enum ImageScene {

    /**
     * 查询场景：外网域名 + 缩略图
     * <p>
     * 用于前端页面展示，需要走 CDN 加速
     */
    QUERY,

    /**
     * 导出场景：根据配置决定内网/外网 + 缩略图
     * <p>
     * 用于 Excel 导出等服务端场景，生产环境走内网提升速度
     */
    EXPORT
}
