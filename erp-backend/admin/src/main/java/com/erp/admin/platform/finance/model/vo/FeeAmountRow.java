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

}
