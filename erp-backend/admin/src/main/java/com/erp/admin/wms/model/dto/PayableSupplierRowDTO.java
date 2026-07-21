package com.erp.admin.wms.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 应付供应商——采购单级原始行（Java 侧按供应商×币种汇总 + 下钻）。
 *
 * @author erp
 */
@Data
public class PayableSupplierRowDTO {

    private Long purchaseOrderId;
    private String orderNo;
    private Long supplierId;
    private String supplierName;
    private String currency;

    /** 合同总额 */
    private BigDecimal totalAmount;
    /** 首付款金额 */
    private BigDecimal prepayAmount;
    /** 首付款状态 0未付/1已付 */
    private Integer prepayStatus;
    /** 尾款状态 0未付/1已付 */
    private Integer balanceStatus;

    private LocalDateTime prepayTime;
    private LocalDateTime balancePayTime;

    private String orderStatus;
}
