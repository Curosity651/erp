package com.erp.admin.platform.finance.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 合同货架预留或正式绑定信息。
 */
@Data
public class ContractRackBindingVO {

    private Long contractId;

    private String contractNo;

    private Long rackAssignmentId;

    private String rackNo;

    private Long wmsTenantId;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private BigDecimal monthlyFee;

    private String contractStatus;

    private String paymentStatus;

}
