package com.erp.admin.wms.service;

import com.erp.admin.common.tenant.WmsTenantContext;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.OperatorDashboardMapper;
import com.erp.admin.wms.mapper.WmsLogisticsProductMapper;
import com.erp.admin.wms.model.entity.WmsLogisticsProduct;
import com.erp.admin.wms.model.vo.OperatorDashboardVO;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WMS 服务商运营数据分析看板（900400）聚合服务。
 *
 * <p>作用域：收入/产品/货主榜按 wms_tenant_id=当前服务商；货主域（吞吐/在库）JOIN 名下限定。
 * 趋势按月（收入流水与支出账单同为 bill_month 口径），区间默认最近 6 个月。</p>
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class OperatorDashboardService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final OperatorDashboardMapper dashboardMapper;

    private final WmsLogisticsProductMapper logisticsProductMapper;

    private final TenantIdentityService tenantIdentityService;

    public OperatorDashboardVO getData(String monthStart, String monthEnd, List<Long> erpTenantIds) {
        Long wmsTenantId = currentOperatorId();

        // 区间兜底：最近 6 个月
        YearMonth end = (monthEnd == null || monthEnd.isEmpty()) ? YearMonth.now() : YearMonth.parse(monthEnd);
        YearMonth start = (monthStart == null || monthStart.isEmpty()) ? end.minusMonths(5)
                : YearMonth.parse(monthStart);
        if (start.isAfter(end)) {
            YearMonth t = start;
            start = end;
            end = t;
        }
        String ms = start.format(MONTH);
        String me = end.format(MONTH);
        List<Long> owners = (erpTenantIds == null || erpTenantIds.isEmpty()) ? null : erpTenantIds;

        OperatorDashboardVO vo = new OperatorDashboardVO();

        // B 收支趋势（按月填轴）
        Map<String, BigDecimal> incomeByMonth = new HashMap<>();
        Map<String, Long> countByMonth = new HashMap<>();
        for (Map<String, Object> row : dashboardMapper.sumIncomeByMonth(wmsTenantId, ms, me, owners)) {
            incomeByMonth.put(str(row.get("billMonth")), dec(row.get("amount")));
            countByMonth.put(str(row.get("billMonth")), num(row.get("cnt")));
        }
        Map<String, BigDecimal> expenseByMonth = new HashMap<>();
        for (Map<String, Object> row : dashboardMapper.sumExpenseByMonth(wmsTenantId, ms, me)) {
            expenseByMonth.put(str(row.get("billMonth")), dec(row.get("amount")));
        }
        OperatorDashboardVO.Trend trend = new OperatorDashboardVO.Trend();
        List<String> months = new ArrayList<>();
        List<BigDecimal> incomeArr = new ArrayList<>();
        List<BigDecimal> expenseArr = new ArrayList<>();
        List<BigDecimal> netArr = new ArrayList<>();
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        long totalCount = 0;
        for (YearMonth m = start; !m.isAfter(end); m = m.plusMonths(1)) {
            String key = m.format(MONTH);
            BigDecimal in = incomeByMonth.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal out = expenseByMonth.getOrDefault(key, BigDecimal.ZERO);
            months.add(key);
            incomeArr.add(in);
            expenseArr.add(out);
            netArr.add(in.subtract(out));
            totalIncome = totalIncome.add(in);
            totalExpense = totalExpense.add(out);
            totalCount += countByMonth.getOrDefault(key, 0L);
        }
        trend.setMonths(months);
        trend.setIncome(incomeArr);
        trend.setExpense(expenseArr);
        trend.setNet(netArr);
        vo.setTrend(trend);

        // A 经营总览
        OperatorDashboardVO.Overview overview = new OperatorDashboardVO.Overview();
        overview.setIncome(totalIncome);
        overview.setIncomeCount(totalCount);
        overview.setExpense(totalExpense);
        overview.setNetProfit(totalIncome.subtract(totalExpense));
        Map<String, Object> ownerCnt = dashboardMapper.countOwners(wmsTenantId);
        overview.setOwnerTotal(num(ownerCnt == null ? null : ownerCnt.get("total")));
        overview.setOwnerEnabled(num(ownerCnt == null ? null : ownerCnt.get("enabled")));
        Map<String, Object> productCnt = dashboardMapper.countProducts(wmsTenantId);
        overview.setProductTotal(num(productCnt == null ? null : productCnt.get("total")));
        overview.setProductEnabled(num(productCnt == null ? null : productCnt.get("enabled")));
        vo.setOverview(overview);

        // C 产品分析（补产品名，取 TOP10）
        List<OperatorDashboardVO.ProductStat> products = dashboardMapper.sumByProduct(wmsTenantId, ms, me, owners);
        Set<Long> pids = products.stream()
            .map(OperatorDashboardVO.ProductStat::getProductId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> nameById = logisticsProductMapper.listByIds(pids)
            .stream()
            .collect(Collectors.toMap(WmsLogisticsProduct::getId, WmsLogisticsProduct::getProductName, (a, b) -> a));
        products.forEach(p -> p.setProductName(nameById.getOrDefault(p.getProductId(), "已删除产品")));
        vo.setProducts(products.size() > 10 ? products.subList(0, 10) : products);

        // D 货主分析
        vo.setOwnerIncomeTop(dashboardMapper.sumIncomeByOwner(wmsTenantId, ms, me, owners));
        vo.setOwnerOutboundTop(dashboardMapper.countOutboundByOwner(wmsTenantId, start.atDay(1),
                end.atEndOfMonth(), owners));

        // E 服务规模
        OperatorDashboardVO.Scale scale = new OperatorDashboardVO.Scale();
        Long onHand = dashboardMapper.sumOwnerOnHand(wmsTenantId, owners);
        scale.setOnHandQty(onHand == null ? 0 : onHand);
        Map<String, Object> racks = dashboardMapper.sumRacks(wmsTenantId, LocalDate.now());
        scale.setRackCount(num(racks == null ? null : racks.get("cnt")));
        scale.setRackMonthlyFee(dec(racks == null ? null : racks.get("fee")));
        vo.setScale(scale);

        return vo;
    }

    // ==================== 辅助 ====================

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static BigDecimal dec(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        return new BigDecimal(String.valueOf(v));
    }

    private static long num(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }

    private Long currentOperatorId() {
        String identityType = tenantIdentityService.currentIdentity(null).getIdentityType();
        if (!TenantIdentityService.IDENTITY_WMS_OPERATOR.equals(identityType)) {
            throw new BusinessException(403, "仅WMS服务商可访问运营数据分析");
        }
        Long wmsTenantId = WmsTenantContext.getCurrentWmsTenant();
        Assert.notNull(wmsTenantId, "服务商上下文缺失");
        return wmsTenantId;
    }

}
