package com.erp.admin.platform.finance.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 操作费按类型汇总行（mapper 结果）。
 *
 * @author erp
 */
@Data
public class FeeAmountRow {

    private String feeType;

    private BigDecimal amount;

    /** 已由复核人确认的补收/冲减净额，不再参与服务商折扣。 */
    private BigDecimal adjustmentAmount;

}
