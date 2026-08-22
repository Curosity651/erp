package com.erp.admin.platform.finance.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 月度应收账单 VO（对齐前端 platform-finance/receivable/types.ts）。
 *
 * @author erp
 */
@Data
@Schema(title = "月度应收账单")
public class MonthlyBillVO {

    private Long id;

    private String billMonth;

    private Long wmsTenantId;

    private String wmsTenantName;

    private BigDecimal rackFee;

    private BigDecimal inboundFee;

    private BigDecimal outboundFee;

    private BigDecimal deliveryFee;

    private BigDecimal returnFee;

    private BigDecimal inspectionFee;

    private BigDecimal driverFee;

    private BigDecimal totalAmount;

    private String status;

    private String confirmedTime;

    private String paidTime;

    private Long paymentVoucherFileId;

    private String remark;

    private List<BillingRecordVO> billingRecords;

}
