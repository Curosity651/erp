package com.erp.admin.platform.finance.service;

import com.erp.admin.platform.finance.mapper.WmsContractFundLedgerMapper;
import com.erp.admin.platform.finance.mapper.WmsContractRackMapper;
import com.erp.admin.platform.finance.mapper.WmsServiceContractMapper;
import com.erp.admin.platform.finance.model.dto.ContractRefundDTO;
import com.erp.admin.platform.finance.model.dto.ServiceContractCreateDTO;
import com.erp.admin.platform.finance.model.entity.WmsContractFundLedger;
import com.erp.admin.platform.finance.model.entity.WmsContractRack;
import com.erp.admin.platform.finance.model.entity.WmsServiceContract;
import com.erp.admin.tenant.service.TenantIdentityService;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.mybatisplus.toolkit.WrappersX;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceContractService {

    private final WmsServiceContractMapper contractMapper;
    private final WmsContractFundLedgerMapper fundMapper;
    private final WmsContractRackMapper contractRackMapper;
    private final TenantIdentityService tenantIdentityService;

    public List<WmsServiceContract> list() {
        assertPlatform();
        return contractMapper.selectList(WrappersX.lambdaQueryX(WmsServiceContract.class)
                .orderByDesc(WmsServiceContract::getCreateTime));
    }

    public List<WmsContractFundLedger> funds(Long contractId) {
        assertPlatform();
        return fundMapper.selectList(WrappersX.lambdaQueryX(WmsContractFundLedger.class)
                .eq(WmsContractFundLedger::getContractId, contractId)
                .orderByDesc(WmsContractFundLedger::getCreateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public WmsServiceContract create(ServiceContractCreateDTO dto) {
        assertPlatform();
        Assert.isTrue(dto.getEndDate().isAfter(dto.getStartDate()), "合同结束日期必须晚于开始日期");
        BigDecimal subscription = nz(dto.getSubscriptionTotal(), new BigDecimal("180000"));
        BigDecimal refundableRate = nz(dto.getRefundableRate(), new BigDecimal("0.70"));
        Assert.isTrue(refundableRate.compareTo(BigDecimal.ZERO) >= 0
                && refundableRate.compareTo(BigDecimal.ONE) <= 0, "可退比例必须在0到1之间");
        BigDecimal refundable = subscription.multiply(refundableRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal service = subscription.subtract(refundable);

        WmsServiceContract contract = new WmsServiceContract();
        contract.setContractNo(dto.getContractNo());
        contract.setWmsTenantId(dto.getWmsTenantId());
        contract.setWarehouseId(dto.getWarehouseId());
        contract.setStartDate(dto.getStartDate());
        contract.setEndDate(dto.getEndDate());
        contract.setRackUnitCount(dto.getRackUnitCount() == null ? 3 : dto.getRackUnitCount());
        contract.setMonthlyRentPerUnit(nz(dto.getMonthlyRentPerUnit(), new BigDecimal("20000")));
        contract.setWarehouseDeposit(nz(dto.getWarehouseDeposit(), new BigDecimal("60000")));
        contract.setSubscriptionTotal(subscription);
        contract.setRefundableRate(refundableRate);
        contract.setRefundableAmount(refundable);
        contract.setServiceAmount(service);
        contract.setMonthlyServiceRecognition(service.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP));
        contract.setContractFileUrl(dto.getContractFileUrl());
        contract.setContractStatus("ACTIVE");
        contract.setRemark(dto.getRemark());
        contractMapper.insert(contract);

        List<String> rackNos = dto.getRackNos() == null ? java.util.Collections.emptyList()
                : dto.getRackNos().stream().filter(org.springframework.util.StringUtils::hasText)
                        .map(String::trim).distinct().collect(java.util.stream.Collectors.toList());
        Assert.isTrue(rackNos.isEmpty() || rackNos.size() == contract.getRackUnitCount(),
                "所选货架排数必须等于合同货架单元数");
        for (String rackNo : rackNos) {
            WmsContractRack rack = new WmsContractRack();
            rack.setContractId(contract.getId());
            rack.setRackNo(rackNo);
            contractRackMapper.insert(rack);
        }

        receipt(contract, "WAREHOUSE_DEPOSIT", contract.getWarehouseDeposit());
        receipt(contract, "SUBSCRIPTION_REFUNDABLE", refundable);
        receipt(contract, "SUBSCRIPTION_SERVICE", service);
        return contract;
    }

    @Transactional(rollbackFor = Exception.class)
    public void recognize(Long contractId, String month) {
        assertPlatform();
        WmsServiceContract contract = contractMapper.selectByIdForUpdate(contractId);
        Assert.notNull(contract, "合同不存在");
        YearMonth ym = YearMonth.parse(month);
        Assert.isTrue(!ym.atEndOfMonth().isBefore(contract.getStartDate())
                && !ym.atDay(1).isAfter(contract.getEndDate()), "账期不在合同有效期内");
        String bizId = "CONTRACT:" + contractId + ":SERVICE:" + month;
        if (exists(bizId)) return;
        ledger(bizId, contract, "SUBSCRIPTION_SERVICE", "RECOGNITION", "IN",
                contract.getMonthlyServiceRecognition(), month, "认购费不可退部分按月确认");
    }

    @Transactional(rollbackFor = Exception.class)
    public void refund(ContractRefundDTO dto) {
        assertPlatform();
        WmsServiceContract contract = contractMapper.selectByIdForUpdate(dto.getContractId());
        Assert.notNull(contract, "合同不存在");
        Assert.isTrue("ACTIVE".equals(contract.getContractStatus()), "仅有效合同可以结算退款");
        Assert.isTrue(!LocalDate.now().isBefore(contract.getStartDate().plusYears(1)),
                "合同履行未满一年，不能办理约定退款");
        Assert.isTrue(Boolean.TRUE.equals(dto.getNoDefault())
                        && Boolean.TRUE.equals(dto.getNoDebt())
                        && Boolean.TRUE.equals(dto.getNoRemainingGoods()),
                "退款前必须完成违约、欠费和遗留货物核验");
        ledger("CONTRACT:" + contract.getId() + ":REFUND:DEPOSIT", contract,
                "WAREHOUSE_DEPOSIT", "REFUND", "OUT", contract.getWarehouseDeposit(),
                YearMonth.now().toString(), dto.getRemark());
        ledger("CONTRACT:" + contract.getId() + ":REFUND:SUBSCRIPTION", contract,
                "SUBSCRIPTION_REFUNDABLE", "REFUND", "OUT", contract.getRefundableAmount(),
                YearMonth.now().toString(), dto.getRemark());
        contract.setContractStatus("SETTLED");
        contractMapper.updateById(contract);
    }

    private void receipt(WmsServiceContract contract, String component, BigDecimal amount) {
        ledger("CONTRACT:" + contract.getId() + ":RECEIPT:" + component, contract,
                component, "RECEIPT", "IN", amount, YearMonth.from(contract.getStartDate()).toString(), "合同收款");
    }

    private void ledger(String bizId, WmsServiceContract contract, String component, String type,
            String direction, BigDecimal amount, String month, String remark) {
        if (exists(bizId)) return;
        WmsContractFundLedger row = new WmsContractFundLedger();
        row.setBizId(bizId);
        row.setContractId(contract.getId());
        row.setWmsTenantId(contract.getWmsTenantId());
        row.setFundComponent(component);
        row.setTransactionType(type);
        row.setDirection(direction);
        row.setAmount(amount);
        row.setCurrency("CNY");
        row.setAccountingMonth(month);
        row.setStatus("POSTED");
        row.setRemark(remark);
        fundMapper.insert(row);
    }

    private boolean exists(String bizId) {
        return fundMapper.selectCount(WrappersX.lambdaQueryX(WmsContractFundLedger.class)
                .eq(WmsContractFundLedger::getBizId, bizId)) > 0;
    }

    private BigDecimal nz(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private void assertPlatform() {
        String identityType = tenantIdentityService.currentIdentity(null).getIdentityType();
        if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(identityType)) {
            throw new BusinessException(403, "仅海外仓平台可管理合同资金");
        }
    }
}
