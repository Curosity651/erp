package com.erp.admin.wms.model.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 采购单未发货批次（采购数量 − 已发货数量），映射为测算模型的 E 在制批次。
 * <p>
 * 口径：采购单状态 ∈ {已确认, 生产中}，未发货量 &gt; 0。ETA 取预计交货日。
 *
 * @author erp
 */
@Data
public class PurchaseUnshippedBatchDTO {

    private String skuCode;

    /** 未发货数量 = 采购数量 − 已发货数量 */
    private Integer quantity;

    /** 预计交货日 → 模型 τE（完工日） */
    private LocalDate expectedDeliveryDate;

    /** 采购单号（批次标签，展示用） */
    private String orderNo;
}
