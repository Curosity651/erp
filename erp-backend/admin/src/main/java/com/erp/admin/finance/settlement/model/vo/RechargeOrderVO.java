package com.erp.admin.finance.settlement.model.vo;

import com.erp.admin.finance.settlement.model.entity.WmsRechargeOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RechargeOrderVO extends WmsRechargeOrder {
    private String wmsTenantName;
    private String erpTenantName;
    private String voucherUrl;
}
