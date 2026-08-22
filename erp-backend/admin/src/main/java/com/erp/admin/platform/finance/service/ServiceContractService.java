package com.erp.admin.platform.finance.service;

import com.erp.admin.platform.finance.mapper.WmsContractFundLedgerMapper;
import com.erp.admin.platform.finance.mapper.WmsContractRackMapper;
import com.erp.admin.platform.finance.mapper.WmsMonthlyBillMapper;
import com.erp.admin.platform.finance.mapper.WmsServiceContractMapper;
import com.erp.admin.platform.finance.model.dto.ContractRefundDTO;
import com.erp.admin.platform.finance.model.dto.ServiceContractCreateDTO;
import com.erp.admin.platform.finance.model.entity.WmsContractFundLedger;
import com.erp.admin.platform.finance.model.entity.WmsContractRack;
import com.erp.admin.platform.finance.model.entity.WmsServiceContract;
import com.erp.admin.platform.finance.model.vo.ContractRefundCheckVO;
import com.erp.admin.system.model.vo.SysFileVO;
import com.erp.admin.system.service.SysFileService;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.mapper.WmsPhysicalInventoryMapper;
import com.erp.admin.wms.mapper.WmsRackAssignmentMapper;
import com.erp.admin.wms.mapper.WarehouseMapper;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.entity.WmsRackAssignment;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.mybatisplus.toolkit.WrappersX;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceContractService {

    private static final String PENDING = "PENDING";
    private static final String PAID = "PAID";
    private static final String REFUNDED = "REFUNDED";
    private static final String CANCELLED = "CANCELLED";
    private static final BigDecimal FIXED_WAREHOUSE_DEPOSIT = new BigDecimal("60000.00");
    private static final BigDecimal SUBSCRIPTION_PER_RACK = new BigDecimal("60000");

    private final WmsServiceContractMapper contractMapper;
    private final WmsContractFundLedgerMapper fundMapper;
    private final WmsContractRackMapper contractRackMapper;
    private final WmsMonthlyBillMapper monthlyBillMapper;
    private final WmsRackAssignmentMapper rackAssignmentMapper;
    private final WarehouseMapper warehouseMapper;
    private final WmsLocationMapper locationMapper;
    private final WmsPhysicalInventoryMapper physicalInventoryMapper;
    private final SysFileService sysFileService;
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
        Assert.notNull(warehouseMapper.selectByIdForUpdate(dto.getWarehouseId()), "仓库不存在");

        int rackCount = dto.getRackUnitCount() == null ? 3 : dto.getRackUnitCount();
        List<String> rackNos = dto.getRackNos() == null ? java.util.Collections.emptyList()
                : dto.getRackNos().stream().filter(StringUtils::hasText).map(String::trim).distinct()
                        .collect(Collectors.toList());
        Assert.isTrue(rackCount > 0, "货架数量必须大于0");
        Assert.isTrue(rackNos.size() == rackCount, "必须选择与合同货架数量一致的实际货架");
        validatePhysicalRacks(dto.getWarehouseId(), rackNos);
        for (String rackNo : rackNos) {
            Assert.isTrue(contractRackMapper.countOverlappingContractBindings(dto.getWarehouseId(), rackNo,
                    dto.getStartDate(), dto.getEndDate(), null) == 0,
                    "货架 " + rackNo + " 已被其他服务合同预留或占用");
            List<WmsRackAssignment> overlaps = rackAssignmentMapper
                    .listByWarehouseAndRackForUpdate(dto.getWarehouseId(), rackNo).stream()
                    .filter(row -> periodsOverlap(dto.getStartDate(), dto.getEndDate(),
                            row.getEffectiveFrom(), row.getEffectiveTo()))
                    .collect(Collectors.toList());
            Assert.isTrue(overlaps.isEmpty(),
                    "货架 " + rackNo + " 在合同期内已通过仓库管理分配，服务合同只能选择空闲货架");
        }

        Assert.notNull(dto.getContractFileId(), "请上传合同PDF文件");
        SysFileVO contractFile = sysFileService.getFileInfo(dto.getContractFileId());
        Assert.notNull(contractFile, "合同文件不存在，请重新上传");
        Assert.isTrue("application/pdf".equalsIgnoreCase(contractFile.getContentType()),
                "服务合同仅支持PDF格式");

        BigDecimal deposit = calculateWarehouseDeposit();
        BigDecimal subscription = calculateSubscriptionTotal(rackCount);
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
        contract.setRackUnitCount(rackCount);
        contract.setMonthlyRentPerUnit(nz(dto.getMonthlyRentPerUnit(), new BigDecimal("20000")));
        contract.setWarehouseDeposit(deposit);
        contract.setSubscriptionTotal(subscription);
        contract.setRefundableRate(refundableRate);
        contract.setRefundableAmount(refundable);
        contract.setServiceAmount(service);
        contract.setMonthlyServiceRecognition(service.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP));
        contract.setContractFileUrl(dto.getContractFileUrl());
        contract.setContractFileId(dto.getContractFileId());
        contract.setContractStatus("DRAFT");
        contract.setPaymentStatus(PENDING);
        contract.setRemark(dto.getRemark());
        contractMapper.insert(contract);

        for (String rackNo : rackNos) {
            WmsContractRack rack = new WmsContractRack();
            rack.setContractId(contract.getId());
            rack.setRackNo(rackNo);
            contractRackMapper.insert(rack);
        }
        return contract;
    }

    @Transactional(rollbackFor = Exception.class)
    public WmsServiceContract receive(Long contractId, String remark) {
        assertPlatform();
        WmsServiceContract contract = contractMapper.selectByIdForUpdate(contractId);
        Assert.notNull(contract, "合同不存在");
        Assert.isTrue(PENDING.equals(contract.getPaymentStatus()), "该合同已经登记过收款");
        Assert.isTrue("DRAFT".equals(contract.getContractStatus()), "只有草稿合同可以确认到账");

        bindContractRacks(contract);
        receipt(contract, "WAREHOUSE_DEPOSIT", contract.getWarehouseDeposit(), remark);
        receipt(contract, "SUBSCRIPTION_REFUNDABLE", contract.getRefundableAmount(), remark);
        receipt(contract, "SUBSCRIPTION_SERVICE", contract.getServiceAmount(), remark);
        contract.setPaymentStatus(PAID);
        contract.setContractStatus("ACTIVE");
        contract.setReceivedTime(LocalDateTime.now());
        contractMapper.updateById(contract);
        return contract;
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long contractId) {
        assertPlatform();
        WmsServiceContract contract = contractMapper.selectByIdForUpdate(contractId);
        Assert.notNull(contract, "合同不存在");
        Assert.isTrue(PENDING.equals(contract.getPaymentStatus())
                && "DRAFT".equals(contract.getContractStatus()), "仅待收款合同可以取消");
        contract.setPaymentStatus(CANCELLED);
        contract.setContractStatus("TERMINATED");
        contractMapper.updateById(contract);
    }

    @Transactional(rollbackFor = Exception.class)
    public void recognize(Long contractId, String month) {
        assertPlatform();
        WmsServiceContract contract = contractMapper.selectByIdForUpdate(contractId);
        Assert.notNull(contract, "合同不存在");
        Assert.isTrue(PAID.equals(contract.getPaymentStatus())
                && "ACTIVE".equals(contract.getContractStatus()), "合同尚未到账或已经结算");
        YearMonth ym = YearMonth.parse(month);
        Assert.isTrue(!ym.atEndOfMonth().isBefore(contract.getStartDate())
                && !ym.atDay(1).isAfter(contract.getEndDate()), "账期不在合同有效期内");
        String bizId = "CONTRACT:" + contractId + ":SERVICE:" + month;
        if (exists(bizId)) {
            return;
        }
        ledger(bizId, contract, "SUBSCRIPTION_SERVICE", "RECOGNITION", "IN",
                contract.getMonthlyServiceRecognition(), month, "认购费不可退部分按月确认");
    }

    public ContractRefundCheckVO refundCheck(Long contractId) {
        assertPlatform();
        WmsServiceContract contract = contractMapper.selectById(contractId);
        Assert.notNull(contract, "合同不存在");

        LocalDate minimumTermDate = contract.getStartDate().plusYears(1);
        LocalDate eligibleDate = contract.getEndDate().isAfter(minimumTermDate)
                ? contract.getEndDate() : minimumTermDate;
        BigDecimal unpaid = nz(monthlyBillMapper.sumUnpaidAmount(contract.getWmsTenantId()), BigDecimal.ZERO);
        List<WmsPhysicalInventory> stock = physicalInventoryMapper.selectList(
                WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
                        .eq(WmsPhysicalInventory::getWmsTenantId, contract.getWmsTenantId())
                        .eq(WmsPhysicalInventory::getWarehouseId, contract.getWarehouseId()));
        long quantity = stock.stream().mapToLong(row -> value(row.getQuantity())).sum();
        long reserved = stock.stream().mapToLong(row -> value(row.getReservedQty())).sum();

        ContractRefundCheckVO result = new ContractRefundCheckVO();
        result.setContractId(contractId);
        result.setEligibleDate(eligibleDate);
        result.setTermSatisfied(!LocalDate.now().isBefore(eligibleDate));
        result.setUnpaidAmount(unpaid);
        result.setNoDebt(unpaid.compareTo(BigDecimal.ZERO) == 0);
        result.setRemainingQuantity(quantity);
        result.setReservedQuantity(reserved);
        result.setNoRemainingGoods(quantity == 0 && reserved == 0);
        result.setRefundable(PAID.equals(contract.getPaymentStatus())
                && "ACTIVE".equals(contract.getContractStatus())
                && result.isTermSatisfied() && result.isNoDebt() && result.isNoRemainingGoods());
        result.setMessage(refundCheckMessage(contract, result));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void refund(ContractRefundDTO dto) {
        assertPlatform();
        WmsServiceContract contract = contractMapper.selectByIdForUpdate(dto.getContractId());
        Assert.notNull(contract, "合同不存在");
        Assert.isTrue(PAID.equals(contract.getPaymentStatus())
                && "ACTIVE".equals(contract.getContractStatus()), "只有已到账的有效合同可以退款");
        ContractRefundCheckVO check = refundCheck(contract.getId());
        Assert.isTrue(check.isTermSatisfied(), "合同尚未达到约定退款日期");
        Assert.isTrue(check.isNoDebt(), "该服务商仍有未结清应收账单");
        Assert.isTrue(check.isNoRemainingGoods(), "合同仓库仍有库存或预占");
        Assert.isTrue(Boolean.TRUE.equals(dto.getNoDefault()), "必须人工确认服务商无违约");

        ledger("CONTRACT:" + contract.getId() + ":REFUND:DEPOSIT", contract,
                "WAREHOUSE_DEPOSIT", "REFUND", "OUT", contract.getWarehouseDeposit(),
                YearMonth.now().toString(), dto.getRemark());
        ledger("CONTRACT:" + contract.getId() + ":REFUND:SUBSCRIPTION", contract,
                "SUBSCRIPTION_REFUNDABLE", "REFUND", "OUT", contract.getRefundableAmount(),
                YearMonth.now().toString(), dto.getRemark());
        closeContractRacks(contract.getId());
        contract.setPaymentStatus(REFUNDED);
        contract.setContractStatus("SETTLED");
        contract.setSettledTime(LocalDateTime.now());
        contractMapper.updateById(contract);
    }

    private void bindContractRacks(WmsServiceContract contract) {
        List<WmsContractRack> contractRacks = contractRackMapper.selectList(
                WrappersX.lambdaQueryX(WmsContractRack.class)
                        .eq(WmsContractRack::getContractId, contract.getId())
                        .orderByAsc(WmsContractRack::getRackNo));
        Assert.isTrue(contractRacks.size() == contract.getRackUnitCount(), "合同货架数量不完整");
        validatePhysicalRacks(contract.getWarehouseId(),
                contractRacks.stream().map(WmsContractRack::getRackNo).collect(Collectors.toList()));

        for (WmsContractRack contractRack : contractRacks) {
            List<WmsRackAssignment> overlaps = rackAssignmentMapper
                    .listByWarehouseAndRackForUpdate(contract.getWarehouseId(), contractRack.getRackNo())
                    .stream()
                    .filter(row -> periodsOverlap(contract.getStartDate(), contract.getEndDate(),
                            row.getEffectiveFrom(), row.getEffectiveTo()))
                    .collect(Collectors.toList());
            Assert.isTrue(overlaps.isEmpty(),
                    "货架 " + contractRack.getRackNo() + " 已通过仓库管理分配，请重新创建合同");
            WmsRackAssignment assignment = new WmsRackAssignment();
            assignment.setWmsTenantId(contract.getWmsTenantId());
            assignment.setWarehouseId(contract.getWarehouseId());
            assignment.setRackNo(contractRack.getRackNo());
            assignment.setMonthlyFee(contract.getMonthlyRentPerUnit());
            assignment.setEffectiveFrom(contract.getStartDate());
            assignment.setEffectiveTo(contract.getEndDate());
            assignment.setContractFileUrl(contract.getContractFileUrl());
            assignment.setRemark("服务合同 " + contract.getContractNo());
            rackAssignmentMapper.insert(assignment);
            contractRack.setRackAssignmentId(assignment.getId());
            contractRackMapper.updateById(contractRack);
        }
    }

    private void closeContractRacks(Long contractId) {
        LocalDate today = LocalDate.now();
        List<WmsContractRack> racks = contractRackMapper.selectList(
                WrappersX.lambdaQueryX(WmsContractRack.class)
                        .eq(WmsContractRack::getContractId, contractId));
        for (WmsContractRack rack : racks) {
            if (rack.getRackAssignmentId() == null) {
                continue;
            }
            WmsRackAssignment assignment = rackAssignmentMapper.selectById(rack.getRackAssignmentId());
            if (assignment != null && (assignment.getEffectiveTo() == null
                    || assignment.getEffectiveTo().isAfter(today))) {
                assignment.setEffectiveTo(today);
                rackAssignmentMapper.updateById(assignment);
            }
        }
    }

    private void validatePhysicalRacks(Long warehouseId, List<String> rackNos) {
        Set<String> physical = locationMapper.listAssignableByWarehouse(warehouseId).stream()
                .map(row -> row.getRackNo()).filter(StringUtils::hasText).collect(Collectors.toSet());
        Set<String> missing = new HashSet<>(rackNos);
        missing.removeAll(physical);
        Assert.isTrue(missing.isEmpty(), "仓库中不存在物理货架: " + String.join(",", missing));
    }

    private String refundCheckMessage(WmsServiceContract contract, ContractRefundCheckVO check) {
        if (!PAID.equals(contract.getPaymentStatus())) {
            return "合同尚未确认到账";
        }
        if (!check.isTermSatisfied()) {
            return "尚未到达约定退款日期 " + check.getEligibleDate();
        }
        if (!check.isNoDebt()) {
            return "存在未结清账单 " + check.getUnpaidAmount() + " 元";
        }
        if (!check.isNoRemainingGoods()) {
            return "仓库仍有库存 " + check.getRemainingQuantity() + " 件、预占 "
                    + check.getReservedQuantity() + " 件";
        }
        return "系统校验通过，确认无违约后可以办理退款";
    }

    private void receipt(WmsServiceContract contract, String component, BigDecimal amount, String remark) {
        ledger("CONTRACT:" + contract.getId() + ":RECEIPT:" + component, contract,
                component, "RECEIPT", "IN", amount,
                YearMonth.now().toString(), StringUtils.hasText(remark) ? remark : "合同收款");
    }

    private void ledger(String bizId, WmsServiceContract contract, String component, String type,
            String direction, BigDecimal amount, String month, String remark) {
        if (exists(bizId)) {
            return;
        }
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

    private boolean periodsOverlap(LocalDate fromA, LocalDate toA, LocalDate fromB, LocalDate toB) {
        LocalDate max = LocalDate.of(9999, 12, 31);
        return !fromA.isAfter(toB == null ? max : toB)
                && !fromB.isAfter(toA == null ? max : toA);
    }

    private BigDecimal nz(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    public static BigDecimal calculateWarehouseDeposit() {
        return FIXED_WAREHOUSE_DEPOSIT;
    }

    public static BigDecimal calculateSubscriptionTotal(int rackCount) {
        return SUBSCRIPTION_PER_RACK.multiply(BigDecimal.valueOf(rackCount));
    }

    private int value(Integer number) {
        return number == null ? 0 : number;
    }

    private void assertPlatform() {
        String identityType = tenantIdentityService.currentIdentity(null).getIdentityType();
        if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(identityType)) {
            throw new BusinessException(403, "仅海外仓平台可管理合同资金");
        }
    }
}
