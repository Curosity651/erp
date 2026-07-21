package com.erp.admin.platform.wildberries.model.response.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Wildberries 订单响应模型
 * <p>
 * 对应 API: GET /api/v3/orders
 * <p>
 * 注意：此接口不返回订单状态（wbStatus、supplierStatus），
 * 需要调用 POST /api/v3/orders/status 单独获取状态
 * 
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WbOrder {
    
    /** 订单ID */
    @JsonProperty("id")
    private Long id;
    
    /** 关联的 Supply ID */
    @JsonProperty("supplyId")
    private String supplyId;
    
    /** 商家仓库ID */
    @JsonProperty("warehouseId")
    private Long warehouseId;
    
    /** WB 办公点ID */
    @JsonProperty("officeId")
    private Long officeId;
    
    /** 配送类型（fbs） */
    @JsonProperty("deliveryType")
    private String deliveryType;
    
    /** 交易ID，同一购物车的订单有相同的 orderUid */
    @JsonProperty("orderUid")
    private String orderUid;

    
    /** 商家货号 */
    @JsonProperty("article")
    private String article;
    
    /** 颜色代码（仅染色产品） */
    @JsonProperty("colorCode")
    private String colorCode;
    
    /** 唯一订单ID（对应其他接口的 srid） */
    @JsonProperty("rid")
    private String rid;
    
    /** 订单创建时间（UTC 时区） */
    @JsonProperty("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime createdAt;
    
    /** 办公点列表（需要配送到的办公点） */
    @JsonProperty("offices")
    @Builder.Default
    private List<String> offices = new ArrayList<>();
    
    /** SKU 列表（条码） */
    @JsonProperty("skus")
    @Builder.Default
    private List<String> skus = new ArrayList<>();
    
    /** WB 商品ID */
    @JsonProperty("nmId")
    private Long nmId;
    
    /** WB 尺码ID */
    @JsonProperty("chrtId")
    private Long chrtId;
    
    /** 售价（分），包含所有折扣（不含 WB 钱包折扣） */
    @JsonProperty("price")
    private Integer price;
    
    /** 转换后价格（分），商家国家货币 */
    @JsonProperty("convertedPrice")
    private Integer convertedPrice;
    
    /** 货币代码（ISO 4217） */
    @JsonProperty("currencyCode")
    private Integer currencyCode;
    
    /** 转换后货币代码（ISO 4217） */
    @JsonProperty("convertedCurrencyCode")
    private Integer convertedCurrencyCode;
    
    /** 货物类型：1=小件, 2=大件, 3=超大件 */
    @JsonProperty("cargoType")
    private Integer cargoType;
    
    /** 买家备注 */
    @JsonProperty("comment")
    private String comment;
    
    /** 验收价格（分），实际验收后显示 */
    @JsonProperty("scanPrice")
    private Integer scanPrice;
    
    /** 是否零库存订单 */
    @JsonProperty("isZeroOrder")
    private Boolean isZeroOrder;
    
    /** 配送地址（可选，仅部分订单有） */
    @JsonProperty("address")
    private WbOrderAddress address;
    
    /** 订单选项 */
    @JsonProperty("options")
    private WbOrderOptions options;
}
