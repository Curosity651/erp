package com.erp.admin.wms.model.vo;

import lombok.Data;

/**
 * 在途库存数量统计 VO
 * <p>
 * 用于替代 Map<String, Object> 返回在途库存统计，提供类型安全
 *
 * @author erp
 */
@Data
public class InTransitQuantityVO {

    /** 区域ID */
    private Long regionId;

    /** SKU编码 */
    private String skuCode;

    /** 在途数量（发货数量 - 已到货数量） */
    private Integer quantity;
}
