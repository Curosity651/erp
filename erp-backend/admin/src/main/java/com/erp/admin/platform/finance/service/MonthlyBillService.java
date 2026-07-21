package com.erp.admin.platform.finance.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.platform.finance.mapper.WmsMonthlyBillMapper;
import com.erp.admin.platform.finance.model.dto.GenerateBillDTO;
import com.erp.admin.platform.finance.model.entity.WmsMonthlyBill;
import com.erp.admin.platform.finance.model.qo.MonthlyBillQO;
import com.erp.admin.platform.finance.model.vo.FeeAmountRow;
import com.erp.admin.platform.finance.model.vo.GenerateBillResultVO;
import com.erp.admin.platform.finance.model.vo.MonthlyBillVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 平台财务·应收账单（链路一 平台→WMS服务商，业务需求 1.7）。
 *
 * <p>货架租金=Σ当月有效 wms_rack_assignment.monthly_fee；操作费=汇总 wms_billing_record(按 wms_tenant_id)，
 * 叠加 wms_tenant_rate_discount 折扣。状态机 DRAFT→CONFIRMED→PAID/DISPUTED（线下对账，手动标记）。
 * 生成/重算：草稿覆盖，已确认/已付款/争议跳过。</p>
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyBillService {

    private static final String DRAFT = "DRAFT";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String PAID = "PAID";
    private static final String DISPUTED = "DISPUTED";

    private final WmsMonthlyBillMapper monthlyBillMapper;

    private final TenantIdentityService tenantIdentityService;

    // ==================== 查询 ====================

    public PageResult<MonthlyBillVO> page(PageParam pageParam, MonthlyBillQO qo) {
        assertPlatform();
        IPage<MonthlyBillVO> page = PageUtil.prodPage(pageParam);
        monthlyBillMapper.pageBills(page, qo);
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    public MonthlyBillVO getDetail(Long id) {
        assertPlatform();
        MonthlyBillVO vo = monthlyBillMapper.selectVoById(id);
        Assert.notNull(vo, "账单不存在");
        return vo;
    }

    // ==================== 生成 / 重算 ====================

    @Transactional(rollbackFor = Exception.class)
    public GenerateBillResultVO generate(GenerateBillDTO dto) {
        assertPlatform();
        GenerateBillResultVO result = generateForMonth(dto.getBillMonth(), dto.getWmsTenantId());
        log.info("账单生成/重算, month={}, tenant={}, result={}", dto.getBillMonth(), dto.getWmsTenantId(), result);
        return result;
    }

    /** 供 /generate 与 月初 cron 复用的核心生成逻辑。 */
    public GenerateBillResultVO generateForMonth(String billMonth, Long wmsTenantId) {
        YearMonth ym = YearMonth.parse(billMonth);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        List<Long> targets = wmsTenantId != null ? java.util.Collections.singletonList(wmsTenantId)
                : monthlyBillMapper.selectWmsOperatorIds();

        GenerateBillResultVO result = new GenerateBillResultVO();
        for (Long tid : targets) {
            WmsMonthlyBill existing = monthlyBillMapper.selectByMonthAndTenant(billMonth, tid);
            // 已确认/已付款/争议 → 跳过（不可被重算覆盖），仅草稿可重算
            if (existing != null && !DRAFT.equals(existing.getStatus())) {
                result.setSkipped(result.getSkipped() + 1);
                continue;
            }
            BigDecimal rackFee = nz(monthlyBillMapper.sumRackFee(tid, monthStart, monthEnd));
            List<FeeAmountRow> rows = monthlyBillMapper.sumFeeByType(tid, billMonth);
            BigDecimal discount = monthlyBillMapper.selectDiscountPct(tid);

            WmsMonthlyBill bill = existing != null ? existing : new WmsMonthlyBill();
            bill.setBillMonth(billMonth);
            bill.setWmsTenantId(tid);
            applyFees(bill, rackFee, rows, discount);
            bill.setStatus(DRAFT);

            if (existing != null) {
                monthlyBillMapper.updateById(bill);
                result.setRecalculated(result.getRecalculated() + 1);
            } else {
                monthlyBillMapper.insert(bill);
                result.setCreated(result.getCreated() + 1);
            }
        }
        return result;
    }

    /**
     * 计费落账（纯逻辑，便于单测）：货架租金原值；6 项操作费按类型归位并叠加折扣；合计=租金+6项。
     * 折扣仅作用于操作费（货架租金为合同月租，不打折）。
     */
    public static void applyFees(WmsMonthlyBill bill, BigDecimal rackFee, List<FeeAmountRow> rows,
            BigDecimal discountPct) {
        BigDecimal factor = BigDecimal.ONE.add(discountPct == null ? BigDecimal.ZERO : discountPct);
        BigDecimal inbound = BigDecimal.ZERO, outbound = BigDecimal.ZERO, delivery = BigDecimal.ZERO;
        BigDecimal ret = BigDecimal.ZERO, inspection = BigDecimal.ZERO, driver = BigDecimal.ZERO;
        if (rows != null) {
            for (FeeAmountRow r : rows) {
                BigDecimal amt = discounted(nz(r.getAmount()), factor);
                switch (r.getFeeType() == null ? "" : r.getFeeType()) {
                    case "INBOUND": inbound = inbound.add(amt); break;
                    case "OUTBOUND": outbound = outbound.add(amt); break;
                    case "DELIVERY": delivery = delivery.add(amt); break;
                    case "RETURN": ret = ret.add(amt); break;
                    case "INSPECTION": inspection = inspection.add(amt); break;
                    case "DRIVER": driver = driver.add(amt); break;
                    default: break;
                }
            }
        }
        BigDecimal rack = nz(rackFee).setScale(2, RoundingMode.HALF_UP);
        bill.setRackFee(rack);
        bill.setInboundFee(inbound);
        bill.setOutboundFee(outbound);
        bill.setDeliveryFee(delivery);
        bill.setReturnFee(ret);
        bill.setInspectionFee(inspection);
        bill.setDriverFee(driver);
        bill.setTotalAmount(rack.add(inbound).add(outbound).add(delivery).add(ret).add(inspection).add(driver));
    }

    // ==================== 状态流转（幂等/前置校验） ====================

    @Transactional(rollbackFor = Exception.class)
    public MonthlyBillVO confirm(Long id) {
        assertPlatform();
        WmsMonthlyBill bill = load(id);
        if (!DRAFT.equals(bill.getStatus()) && !DISPUTED.equals(bill.getStatus())) {
            throw new BusinessException(400, "仅草稿或争议账单可确认");
        }
        bill.setStatus(CONFIRMED);
        bill.setConfirmedTime(LocalDateTime.now());
        bill.setRemark(null);
        monthlyBillMapper.updateById(bill);
        return monthlyBillMapper.selectVoById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public MonthlyBillVO pay(Long id) {
        assertPlatform();
        WmsMonthlyBill bill = load(id);
        if (!CONFIRMED.equals(bill.getStatus())) {
            throw new BusinessException(400, "仅已确认账单可标记付款");
        }
        bill.setStatus(PAID);
        bill.setPaidTime(LocalDateTime.now());
        monthlyBillMapper.updateById(bill);
        return monthlyBillMapper.selectVoById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public MonthlyBillVO dispute(Long id, String remark) {
        assertPlatform();
        WmsMonthlyBill bill = load(id);
        if (!CONFIRMED.equals(bill.getStatus())) {
            throw new BusinessException(400, "仅已确认账单可标记争议");
        }
        bill.setStatus(DISPUTED);
        bill.setRemark(remark);
        monthlyBillMapper.updateById(bill);
        return monthlyBillMapper.selectVoById(id);
    }

    // ==================== 辅助 ====================

    private WmsMonthlyBill load(Long id) {
        WmsMonthlyBill bill = monthlyBillMapper.selectById(id);
        Assert.notNull(bill, "账单不存在");
        return bill;
    }

    private static BigDecimal discounted(BigDecimal base, BigDecimal factor) {
        return base.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private void assertPlatform() {
        String identityType = tenantIdentityService.currentIdentity(null).getIdentityType();
        if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(identityType)) {
            throw new BusinessException(403, "仅海外仓平台可访问平台财务");
        }
    }

}
