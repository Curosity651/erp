package com.erp.admin.finance.settlement.mapper;

import com.erp.admin.finance.settlement.model.qo.FundSettlementQO;
import com.erp.admin.finance.settlement.model.vo.FundAccountVO;
import com.erp.admin.finance.settlement.model.vo.FundLedgerVO;
import com.erp.admin.finance.settlement.model.vo.FundLedgerMonthStatusVO;
import com.erp.admin.finance.settlement.model.vo.RechargeOrderVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FundSettlementQueryMapper {
    List<FundAccountVO> ownerAccounts(@Param("erpTenantId") Long erpTenantId);
    List<FundAccountVO> operatorOwnerAccounts(@Param("wmsTenantId") Long wmsTenantId,
            @Param("qo") FundSettlementQO qo);
    List<FundAccountVO> operatorPlatformAccounts(@Param("wmsTenantId") Long wmsTenantId);
    List<FundAccountVO> platformProviderAccounts(@Param("qo") FundSettlementQO qo);
    List<RechargeOrderVO> recharges(@Param("scope") String scope,
            @Param("wmsTenantId") Long wmsTenantId, @Param("erpTenantId") Long erpTenantId,
            @Param("qo") FundSettlementQO qo);
    List<FundLedgerVO> ownerLedger(@Param("wmsTenantId") Long wmsTenantId,
            @Param("erpTenantId") Long erpTenantId, @Param("qo") FundSettlementQO qo);
    List<FundLedgerVO> platformLedger(@Param("wmsTenantId") Long wmsTenantId,
            @Param("qo") FundSettlementQO qo);
    List<FundLedgerMonthStatusVO> platformStatementStatuses(@Param("wmsTenantId") Long wmsTenantId,
            @Param("currency") String currency);
}
