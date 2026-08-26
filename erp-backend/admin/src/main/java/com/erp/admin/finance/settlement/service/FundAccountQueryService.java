package com.erp.admin.finance.settlement.service;

import com.erp.admin.common.tenant.WmsTenantContext;
import com.erp.admin.finance.settlement.mapper.FundSettlementQueryMapper;
import com.erp.admin.finance.settlement.model.enums.RechargeAccountScope;
import com.erp.admin.finance.settlement.model.qo.FundSettlementQO;
import com.erp.admin.finance.settlement.model.vo.FundAccountVO;
import com.erp.admin.finance.settlement.model.vo.FundLedgerVO;
import com.erp.admin.finance.settlement.model.vo.FundLedgerMonthVO;
import com.erp.admin.finance.settlement.model.vo.FundLedgerMonthStatusVO;
import com.erp.admin.finance.settlement.model.vo.RechargeOrderVO;
import com.erp.admin.system.service.SysFileService;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FundAccountQueryService {
    private final FundSettlementQueryMapper queryMapper;
    private final TenantIdentityService identityService;
    private final SysFileService fileService;
    private final FundLedgerPresentationService ledgerPresentationService;

    public List<FundAccountVO> ownerAccounts() {
        return queryMapper.ownerAccounts(identity(TenantIdentityService.IDENTITY_ERP_USER).getTenantId());
    }

    public List<RechargeOrderVO> ownerRecharges(FundSettlementQO qo) {
        TenantIdentityVO i = identity(TenantIdentityService.IDENTITY_ERP_USER);
        return enrich(queryMapper.recharges(RechargeAccountScope.OWNER_ACCOUNT.name(), null, i.getTenantId(), qo));
    }

    public List<FundLedgerVO> ownerLedger(FundSettlementQO qo) {
        TenantIdentityVO i = identity(TenantIdentityService.IDENTITY_ERP_USER);
        List<FundAccountVO> accounts = queryMapper.ownerAccounts(i.getTenantId());
        Long wmsId = accounts.isEmpty() ? null : accounts.get(0).getWmsTenantId();
        if (wmsId == null) return java.util.Collections.emptyList();
        return queryMapper.ownerLedger(wmsId, i.getTenantId(), qo);
    }

    public List<FundAccountVO> operatorOwnerAccounts(FundSettlementQO qo) {
        return queryMapper.operatorOwnerAccounts(operatorId(), qo);
    }

    public List<RechargeOrderVO> operatorOwnerRecharges(FundSettlementQO qo) {
        return enrich(queryMapper.recharges(RechargeAccountScope.OWNER_ACCOUNT.name(), operatorId(),
                qo.getErpTenantId(), qo));
    }

    public List<FundLedgerVO> operatorOwnerLedger(Long erpTenantId, FundSettlementQO qo) {
        if (erpTenantId == null) throw new BusinessException(400, "请选择货主");
        return queryMapper.ownerLedger(operatorId(), erpTenantId, qo);
    }

    public List<FundAccountVO> operatorPlatformAccounts() {
        return queryMapper.operatorPlatformAccounts(operatorId());
    }

    public List<RechargeOrderVO> operatorPlatformRecharges(FundSettlementQO qo) {
        return enrich(queryMapper.recharges(RechargeAccountScope.PLATFORM_ACCOUNT.name(), operatorId(), null, qo));
    }

    public List<FundLedgerVO> operatorPlatformLedger(FundSettlementQO qo) {
        return queryMapper.platformLedger(operatorId(), qo);
    }

    public List<FundAccountVO> platformAccounts(FundSettlementQO qo) {
        identity(TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM);
        return queryMapper.platformProviderAccounts(qo);
    }

    public List<RechargeOrderVO> platformRecharges(FundSettlementQO qo) {
        identity(TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM);
        return enrich(queryMapper.recharges(RechargeAccountScope.PLATFORM_ACCOUNT.name(), qo.getWmsTenantId(), null, qo));
    }

    public List<FundLedgerVO> platformLedger(Long wmsTenantId, FundSettlementQO qo) {
        identity(TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM);
        if (wmsTenantId == null) throw new BusinessException(400, "请选择WMS服务商");
        return queryMapper.platformLedger(wmsTenantId, qo);
    }

    public List<FundLedgerMonthVO> platformLedgerMonths(Long wmsTenantId, FundSettlementQO qo) {
        identity(TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM);
        if (wmsTenantId == null) throw new BusinessException(400, "请选择WMS服务商");
        FundSettlementQO allRowsQuery = new FundSettlementQO();
        allRowsQuery.setCurrency(qo.getCurrency());
        List<FundLedgerVO> rows = queryMapper.platformLedger(wmsTenantId, allRowsQuery);
        Map<String, String> statuses = queryMapper.platformStatementStatuses(wmsTenantId, qo.getCurrency()).stream()
                .collect(Collectors.toMap(FundLedgerMonthStatusVO::getMonth,
                        FundLedgerMonthStatusVO::getStatus, (left, right) -> left));
        return ledgerPresentationService.group(rows, statuses, LocalDate.now(), qo.getStartDate(), qo.getEndDate());
    }

    private List<RechargeOrderVO> enrich(List<RechargeOrderVO> rows) {
        rows.forEach(row -> {
            try { row.setVoucherUrl(fileService.getDownloadUrl(row.getVoucherFileId())); }
            catch (RuntimeException ignored) { row.setVoucherUrl(null); }
        });
        return rows;
    }

    private Long operatorId() {
        TenantIdentityVO i = identity(TenantIdentityService.IDENTITY_WMS_OPERATOR);
        return WmsTenantContext.getCurrentWmsTenant() == null ? i.getTenantId()
                : WmsTenantContext.getCurrentWmsTenant();
    }

    private TenantIdentityVO identity(String expected) {
        TenantIdentityVO i = identityService.currentIdentity(null);
        if (!expected.equals(i.getIdentityType())) throw new BusinessException(403, "当前身份无权访问该资金账户");
        return i;
    }
}
