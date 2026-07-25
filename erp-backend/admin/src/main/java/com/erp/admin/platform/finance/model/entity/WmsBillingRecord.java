package com.erp.admin.platform.finance.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 操作费流水（链路一操作费来源，按 wms_tenant_id 汇总）。按 biz_id 幂等。
 *
 * @author erp
 */
@Data
@TableName("wms_billing_record")
@Schema(title = "操作费流水")
public class WmsBillingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(title = "业务幂等键")
    private String bizId;

    @Schema(title = "WMS服务商")
    private Long wmsTenantId;

    @Schema(title = "货主(操作量归集来源)")
    private Long erpTenantId;

    private Long warehouseId;

    @Schema(title = "账期 YYYY-MM")
    private String billMonth;

    @Schema(title = "INBOUND/OUTBOUND/DELIVERY/RETURN/INSPECTION/DRIVER")
    private String feeType;

    private String sourceType;

    private Long sourceId;

    private String feeCode;

    private String billingUnit;

    private Integer quantity;

    private BigDecimal billingQuantity;

    private BigDecimal unitPrice;

    private BigDecimal baseAmount;

    private BigDecimal amount;

    private String currency;

    private String chargeStatus;

    private Long reversedRecordId;

    private String rateSnapshot;

    private String sourceRef;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
