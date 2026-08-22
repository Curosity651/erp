package com.erp.admin.wms.model.entity;

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
 * 客户计费流水（链路二物流费 / 操作费等）。按 biz_id 幂等（唯一键），供月度账单汇总。操作流水表，不加 deleted。
 *
 * @author erp
 */
@Data
@TableName("wms_client_billing_record")
@Schema(title = "客户计费流水")
public class WmsClientBillingRecord {

    @TableId(type = IdType.AUTO)
    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "业务幂等键(如 SHIP:{outboundOrderId})")
    private String bizId;

    @Schema(title = "货架归属(WMS服务商)，无则0")
    private Long wmsTenantId;

    @Schema(title = "货主")
    private Long erpTenantId;

    @Schema(title = "关联出库单")
    private Long outboundOrderId;

    @Schema(title = "关联履约订单")
    private Long fulfillmentOrderId;

    @Schema(title = "费用类型 SHIPPING/OPERATION")
    private String feeType;

    @Schema(title = "金额")
    private BigDecimal amount;

    @Schema(title = "币种")
    private String currency;

    @Schema(title = "面单跟踪号")
    private String trackingNo;

    @Schema(title = "物流产品ID")
    private Long logisticsProductId;

    @Schema(title = "物流产品名称快照")
    private String productNameSnapshot;

    @Schema(title = "物流产品说明快照")
    private String productDescriptionSnapshot;

    @Schema(title = "账期 YYYY-MM")
    private String billMonth;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "创建时间")
    private LocalDateTime createTime;

}
