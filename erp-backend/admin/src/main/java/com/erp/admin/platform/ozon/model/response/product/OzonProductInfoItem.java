package com.erp.admin.platform.ozon.model.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Ozon 商品详情项（精简版）
 * <p>
 * 来源：POST /v3/product/info/list 响应
 * <p>
 * <b>重要说明：</b>本类只封装了业务所需的核心字段。
 * API 实际返回的字段远比此处封装的要多，完整字段列表如下：
 * <p>
 * <b>基础信息：</b>
 * <ul>
 *   <li>id - 商品 ID</li>
 *   <li>sku - Ozon SKU（用于 FBO 库存查询）</li>
 *   <li>offer_id - 卖家商品编码</li>
 *   <li>name - 商品名称</li>
 *   <li>barcodes - 条形码列表</li>
 *   <li>description_category_id - 分类 ID</li>
 *   <li>type_id - 商品类型 ID</li>
 *   <li>created_at - 创建时间</li>
 *   <li>updated_at - 更新时间</li>
 * </ul>
 * <p>
 * <b>状态信息：</b>
 * <ul>
 *   <li>is_archived - 是否手动归档</li>
 *   <li>is_autoarchived - 是否自动归档</li>
 *   <li>is_discounted - 是否为折扣商品</li>
 *   <li>is_kgt - 是否为大件商品</li>
 *   <li>is_super - 是否为 Super 商品</li>
 *   <li>has_discounted_fbo_item - 是否有折扣 FBO 商品</li>
 *   <li>statuses - 商品状态详情（is_created, moderate_status, validation_state 等）</li>
 *   <li>visibility_details - 可见性详情</li>
 *   <li>errors - 创建/验证错误列表</li>
 * </ul>
 * <p>
 * <b>价格信息：</b>
 * <ul>
 *   <li>price - 当前售价</li>
 *   <li>old_price - 原价（划线价）</li>
 *   <li>min_price - 最低价</li>
 *   <li>currency_code - 货币代码</li>
 *   <li>vat - 增值税率</li>
 *   <li>price_indexes - 价格指数</li>
 *   <li>commissions - 佣金详情</li>
 * </ul>
 * <p>
 * <b>库存信息（FBS/rFBS/FBP，不含 FBO）：</b>
 * <ul>
 *   <li>stocks.has_stock - 是否有库存</li>
 *   <li>stocks.stocks[].sku - SKU</li>
 *   <li>stocks.stocks[].present - 当前库存</li>
 *   <li>stocks.stocks[].reserved - 预留库存</li>
 *   <li>stocks.stocks[].source - 销售方案（FBS/rFBS/FBP）</li>
 *   <li>discounted_fbo_stocks - 折扣 FBO 库存</li>
 * </ul>
 * <p>
 * <b>图片信息：</b>
 * <ul>
 *   <li>images - 图片 URL 列表</li>
 *   <li>images360 - 360 度图片列表</li>
 *   <li>primary_image - 主图</li>
 *   <li>color_image - 颜色图</li>
 * </ul>
 * <p>
 * <b>其他信息：</b>
 * <ul>
 *   <li>volume_weight - 体积重量</li>
 *   <li>availabilities - 可用性信息</li>
 *   <li>sources - 商品来源详情</li>
 *   <li>promotions - 促销信息</li>
 *   <li>model_info - 模型信息</li>
 * </ul>
 * <p>
 * <b>注意：</b>此接口返回的 stocks 字段仅包含 FBS/rFBS/FBP 库存。
 * 获取 FBO 库存必须使用 /v1/analytics/stocks 接口（需要传入 sku 参数）。
 *
 * @see <a href="https://docs.ozon.ru/api/seller/#operation/ProductAPI_GetProductInfoList">API 文档</a>
 */
@Data
public class OzonProductInfoItem {

    /**
     * 商品 ID（Ozon 内部标识）
     */
    @JsonProperty("id")
    private Long id;

    /**
     * Ozon SKU
     * <p>
     * 用于调用 /v1/analytics/stocks 获取 FBO 库存
     */
    @JsonProperty("sku")
    private Long sku;

    /**
     * 卖家商品编码（用于 SKU 映射）
     */
    @JsonProperty("offer_id")
    private String offerId;

    /**
     * 商品名称
     */
    @JsonProperty("name")
    private String name;

    /**
     * 条形码列表
     */
    @JsonProperty("barcodes")
    private String[] barcodes;

    /**
     * 是否已归档（手动）
     */
    @JsonProperty("is_archived")
    private Boolean isArchived;

    /**
     * 是否已自动归档
     */
    @JsonProperty("is_autoarchived")
    private Boolean isAutoarchived;

    /**
     * 是否为折扣商品
     */
    @JsonProperty("is_discounted")
    private Boolean isDiscounted;

    /**
     * 创建时间（ISO 8601 格式）
     */
    @JsonProperty("created_at")
    private String createdAt;

    /**
     * 更新时间（ISO 8601 格式）
     */
    @JsonProperty("updated_at")
    private String updatedAt;

}
