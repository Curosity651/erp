package com.erp.admin.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.common.tenant.WmsTenantContext;
import com.erp.admin.platform.finance.mapper.WmsMonthlyBillMapper;
import com.erp.admin.platform.finance.model.qo.MonthlyBillQO;
import com.erp.admin.platform.finance.model.vo.MonthlyBillVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.OperatorFinanceMapper;
import com.erp.admin.wms.mapper.WmsLogisticsProductMapper;
import com.erp.admin.wms.model.entity.WmsLogisticsProduct;
import com.erp.admin.wms.model.vo.OperatorIncomeVO;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * WMS 服务商财务视图。
 *
 * <p><b>收入</b>（链路二）：名下货主使用物流产品，按次×单价计费 —— 聚合
 * {@code wms_client_billing_record}（wms_tenant_id=当前服务商）。
 * <b>支出</b>（链路一）：服务商使用平台仓库服务应付款 —— 只读
 * {@code wms_monthly_bill}（服务商侧对账视图，平台不介入管理，无状态操作）。</p>
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class OperatorFinanceService {

    private final OperatorFinanceMapper operatorFinanceMapper;

    private final WmsLogisticsProductMapper logisticsProductMapper;

    private final WmsMonthlyBillMapper monthlyBillMapper;

    private final TenantIdentityService tenantIdentityService;

    // ==================== 收入 ====================

    public OperatorIncomeVO.Summary incomeSummary(String monthStart, String monthEnd, Long erpTenantId) {
        Long wmsTenantId = currentOperatorId();
        List<OperatorIncomeVO.SummaryRow> rows = operatorFinanceMapper.sumIncomeByProductMonth(wmsTenantId,
                monthStart, monthEnd, erpTenantId);
        enrichProductInfo(rows);

        OperatorIncomeVO.Summary summary = new OperatorIncomeVO.Summary();
        summary.setRows(rows);
        Map<String, BigDecimal> totals = new TreeMap<>();
        for (OperatorIncomeVO.SummaryRow row : rows) {
            String currency = normalizeCurrency(row.getCurrency());
            row.setCurrency(currency);
            totals.merge(currency, row.getSubtotal() == null ? BigDecimal.ZERO : row.getSubtotal(), BigDecimal::add);
        }
        summary.setCurrencyTotals(totals.entrySet().stream()
            .map(entry -> new OperatorIncomeVO.CurrencyTotal(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList()));
        summary.setTotalCount(rows.stream().mapToLong(r -> r.getUsageCount() == null ? 0 : r.getUsageCount()).sum());
        return summary;
    }

    public List<OperatorIncomeVO.Record> incomeRecords(String monthStart, String monthEnd, Long erpTenantId,
            Long productId) {
        Long wmsTenantId = currentOperatorId();
        List<OperatorIncomeVO.Record> records = operatorFinanceMapper.listIncomeRecords(wmsTenantId, monthStart,
                monthEnd, erpTenantId, productId);
        records.forEach(r -> r.setCurrency(normalizeCurrency(r.getCurrency())));
        return records;
    }

    // ==================== 支出（只读对账视图） ====================

    /**
     * 服务商支出账单分页：强制 wmsTenantId=自己（忽略前端传入），复用平台账单读模型。
     */
    public PageResult<MonthlyBillVO> expensePage(PageParam pageParam, MonthlyBillQO qo) {
        Long wmsTenantId = currentOperatorId();
        qo.setWmsTenantId(wmsTenantId);
        IPage<MonthlyBillVO> page = PageUtil.prodPage(pageParam);
        monthlyBillMapper.pageBills(page, qo);
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    public MonthlyBillVO expenseDetail(Long id) {
        Long wmsTenantId = currentOperatorId();
        MonthlyBillVO vo = monthlyBillMapper.selectVoById(id);
        Assert.notNull(vo, "账单不存在");
        if (!wmsTenantId.equals(vo.getWmsTenantId())) {
            throw new BusinessException(403, "无权查看其它服务商的账单");
        }
        return vo;
    }

    // ==================== 辅助 ====================

    private void enrichProductInfo(List<OperatorIncomeVO.SummaryRow> rows) {
        Set<Long> ids = rows.stream()
            .map(OperatorIncomeVO.SummaryRow::getProductId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, WmsLogisticsProduct> byId = logisticsProductMapper.listByIds(ids)
            .stream()
            .collect(Collectors.toMap(WmsLogisticsProduct::getId, p -> p, (a, b) -> a));
        for (OperatorIncomeVO.SummaryRow row : rows) {
            WmsLogisticsProduct p = row.getProductId() == null ? null : byId.get(row.getProductId());
            if (p != null) {
                row.setProductName(p.getProductName());
                row.setTags(WmsLogisticsProductService.parseTags(p.getTags()));
                row.setUnitPrice(p.getUnitPrice());
            } else {
                row.setProductName("已删除产品");
                row.setTags(java.util.Collections.emptyList());
            }
        }
    }

    private Long currentOperatorId() {
        String identityType = tenantIdentityService.currentIdentity(null).getIdentityType();
        if (!TenantIdentityService.IDENTITY_WMS_OPERATOR.equals(identityType)) {
            throw new BusinessException(403, "仅WMS服务商可访问服务商财务");
        }
        Long wmsTenantId = WmsTenantContext.getCurrentWmsTenant();
        Assert.notNull(wmsTenantId, "服务商上下文缺失");
        return wmsTenantId;
    }

    private static String normalizeCurrency(String currency) {
        return currency == null || currency.trim().isEmpty()
                ? "UNKNOWN"
                : currency.trim().toUpperCase(Locale.ROOT);
    }

}
