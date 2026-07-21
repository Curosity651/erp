package com.erp.admin.financial.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.financial.mapper.OrderReconciliationMapper;
import com.erp.admin.financial.model.enums.ReconciliationStatus;
import com.erp.admin.financial.model.qo.OrderReconciliationQO;
import com.erp.admin.financial.model.vo.OrderReconciliationDetailVO;
import com.erp.admin.financial.model.vo.OrderReconciliationExportVO;
import com.erp.admin.financial.model.vo.OrderReconciliationStatsVO;
import com.erp.admin.financial.model.vo.OrderReconciliationVO;
import com.erp.admin.order.mapper.ErpOrderItemMapper;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.ErpOrderItem;
import com.erp.admin.order.model.vo.OrderItemVO;
import com.erp.admin.order.model.enums.ErpOrderStatusEnum;
import com.erp.admin.product.model.vo.SkuBriefVO;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.product.service.SkuMappingService;
import com.erp.admin.shop.mapper.ShopMapper;
import com.erp.admin.shop.model.entity.Shop;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 订单财务对账 Service
 * <p>
 * 提供订单财务对账的核心业务逻辑：
 * - 订单对账统计
 * - 订单对账列表查询
 * - 订单财务详情查询
 * - 孤立财务记录统计与查询
 * - 数据导出
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReconciliationService {

    private final OrderReconciliationMapper orderReconciliationMapper;

    private final ErpOrderMapper erpOrderMapper;

    private final ShopMapper shopMapper;

    private final ObjectMapper objectMapper;

	private final SkuBriefService skuBriefService;

    private final SkuMappingService skuMappingService;

    private final ErpOrderItemMapper orderItemMapper;

    // ==================== 订单财务对账相关 ====================

    /**
     * 获取订单对账统计数据
     *
     * @param qo 查询条件
     * @return 统计数据
     */
    public OrderReconciliationStatsVO getOrderStats(OrderReconciliationQO qo) {
        OrderReconciliationStatsVO stats = orderReconciliationMapper.selectOrderStats(qo);
        if (stats == null) {
            return OrderReconciliationStatsVO.empty();
        }

        return stats;
    }

    /**
     * 分页查询订单对账列表
     *
     * @param pageParam 分页参数
     * @param qo        查询条件
     * @return 分页结果
     */
    public PageResult<OrderReconciliationVO> pageOrderReconciliation(PageParam pageParam, OrderReconciliationQO qo) {
        IPage<OrderReconciliationVO> page = PageUtil.prodPage(pageParam);
        page = orderReconciliationMapper.selectOrderReconciliationPage(page, qo);
        List<OrderReconciliationVO> records = page.getRecords();

        if (!records.isEmpty()) {
            // 批量加载 items
            List<Long> orderIds = records.stream()
                    .map(OrderReconciliationVO::getOrderId)
                    .collect(Collectors.toList());
            List<ErpOrderItem> allItems = orderItemMapper.selectByOrderIds(orderIds);
            Map<Long, List<ErpOrderItem>> itemMap = allItems.stream()
                    .collect(Collectors.groupingBy(ErpOrderItem::getOrderId));

            // 通过 platformItemId 查询 skuCode 映射，再批量获取 SKU 信息
            Set<String> platformItemIds = allItems.stream()
                    .map(ErpOrderItem::getPlatformItemId)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
            Map<String, String> skuCodeMap = skuMappingService
                    .getSkuCodeMapByPlatformItemIds(platformItemIds);
            Set<String> skuCodes = new HashSet<>(skuCodeMap.values());
            Map<String, SkuBriefVO> skuBriefMap = skuBriefService.buildMapForQuery(skuCodes);

            // 填充 items 到 VO
            for (OrderReconciliationVO vo : records) {
                List<ErpOrderItem> orderItems = itemMap.getOrDefault(vo.getOrderId(), Collections.emptyList());
                vo.setItems(buildOrderItemVOs(orderItems, skuBriefMap, skuCodeMap));

                // 兼容：首个 item 填充 skuBrief
                if (!orderItems.isEmpty()) {
                    String firstSkuCode = skuCodeMap.get(orderItems.get(0).getPlatformItemId());
                    if (StringUtils.hasText(firstSkuCode)) {
                        vo.setSkuBrief(skuBriefMap.get(firstSkuCode));
                    }
                }
            }
        }

        return new PageResult<>(records, page.getTotal());
    }

    /**
     * 获取订单财务对账详情
     *
     * @param orderId    订单ID
     * @param periodType 报表类型
     * @return 订单对账详情
     */
    public OrderReconciliationDetailVO getOrderReconciliationDetail(Long orderId, String periodType) {
        // 1. 查询订单基本信息
        ErpOrder order = erpOrderMapper.selectById(orderId);
        if (order == null) {
            return null;
        }

        OrderReconciliationDetailVO detail = new OrderReconciliationDetailVO();

        // 2. 填充订单基本信息
        detail.setOrderId(order.getId());
        detail.setPlatformOrderId(order.getPlatformOrderId());
        detail.setRid(extractRidFromRawJson(order.getRawJson()));
        detail.setShopId(order.getShopId());
        detail.setFulfillmentType(order.getFulfillmentType());
        detail.setTotalAmountRub(order.getTotalAmountRub());
        detail.setOrderTimeMoscow(order.getPlatformCreatedAtMoscow());

        // 3. 填充状态信息
        detail.setErpStatus(order.getErpStatus());
        detail.setPlatformStatus(order.getPlatformStatus());
        detail.setPlatformSubstatus(order.getPlatformSubstatus());

        // 4. 填充店铺名称
        if (order.getShopId() != null) {
            Shop shop = shopMapper.selectById(order.getShopId());
            if (shop != null) {
                detail.setShopName(shop.getName());
            }
        }

        // 5. 加载 items 并填充 SKU 信息
        List<ErpOrderItem> items = orderItemMapper.selectByOrderId(order.getId());
        Set<String> platformItemIds = items.stream()
                .map(ErpOrderItem::getPlatformItemId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, String> skuCodeMap = skuMappingService
                .getSkuCodeMapByPlatformItemIds(platformItemIds);
        Set<String> skuCodes = new HashSet<>(skuCodeMap.values());
        Map<String, SkuBriefVO> skuBriefMap = skuBriefService.buildMapForQuery(skuCodes);
        detail.setItems(buildOrderItemVOs(items, skuBriefMap, skuCodeMap));

        // 兼容旧字段
        if (!items.isEmpty()) {
            String firstSkuCode = skuCodeMap.get(items.get(0).getPlatformItemId());
            if (StringUtils.hasText(firstSkuCode)) {
                detail.setSkuBrief(skuBriefMap.get(firstSkuCode));
            }
        }

        // 6. 查询关联的财务记录
        List<OrderReconciliationDetailVO.FinancialRecordItemVO> financialRecords =
                orderReconciliationMapper.selectOrderFinancialRecords(
                        order.getPlatformOrderId(),
                        order.getShopId(),
                        periodType);

        detail.setFinancialRecords(financialRecords);

        // 7. 计算财务汇总
        OrderReconciliationDetailVO.FinancialSummary summary = calculateFinancialSummary(financialRecords);
        detail.setFinancialSummary(summary);

        // 8. 判断对账状态
        String reconciliationStatus = determineReconciliationStatus(order, financialRecords);
        detail.setReconciliationStatus(reconciliationStatus);

        return detail;
    }

    /**
     * 导出订单对账数据
     *
     * @param qo 查询条件
     * @return 导出数据列表
     */
    public List<OrderReconciliationExportVO> exportOrderReconciliation(OrderReconciliationQO qo) {
        List<OrderReconciliationVO> records = orderReconciliationMapper.selectOrderReconciliationForExport(qo);

        if (!records.isEmpty()) {
            // 批量加载 items
            List<Long> orderIds = records.stream()
                    .map(OrderReconciliationVO::getOrderId)
                    .collect(Collectors.toList());
            List<ErpOrderItem> allItems = orderItemMapper.selectByOrderIds(orderIds);
            Map<Long, List<ErpOrderItem>> itemMap = allItems.stream()
                    .collect(Collectors.groupingBy(ErpOrderItem::getOrderId));

            Set<String> platformItemIds = allItems.stream()
                    .map(ErpOrderItem::getPlatformItemId)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
            Map<String, String> skuCodeMap = skuMappingService
                    .getSkuCodeMapByPlatformItemIds(platformItemIds);
            Set<String> skuCodes = new HashSet<>(skuCodeMap.values());
            Map<String, SkuBriefVO> skuBriefMap = skuBriefService.buildMapForExport(skuCodes);

            for (OrderReconciliationVO vo : records) {
                List<ErpOrderItem> orderItems = itemMap.getOrDefault(vo.getOrderId(), Collections.emptyList());
                vo.setItems(buildOrderItemVOs(orderItems, skuBriefMap, skuCodeMap));
            }
        }

        // flatMap 导出：一个订单多商品展开为多行
        return records.stream()
                .flatMap(vo -> {
                    if (vo.getItems() == null || vo.getItems().isEmpty()) {
                        return Stream.of(convertToExportVO(vo, null));
                    }
                    return vo.getItems().stream().map(item -> convertToExportVO(vo, item));
                })
                .collect(Collectors.toList());
    }

    /**
     * 将 OrderReconciliationVO 转换为 OrderReconciliationExportVO
     *
     * @param vo   订单对账 VO
     * @param item 订单商品明细（多商品展开时每个 item 一行，无商品时传 null）
     * @return 导出 VO
     */
    private OrderReconciliationExportVO convertToExportVO(OrderReconciliationVO vo, OrderItemVO item) {
        OrderReconciliationExportVO exportVO = new OrderReconciliationExportVO();

        // 店铺与订单基础信息
        exportVO.setShopName(vo.getShopName());
        exportVO.setPlatformOrderId(vo.getPlatformOrderId());
        exportVO.setRid(vo.getRid());

        // SKU 信息从 item 读取
        if (item != null) {
            exportVO.setArticle(item.getPlatformItemId());
            exportVO.setQuantity(item.getQuantity());
            if (StringUtils.hasText(item.getSkuCode())) {
                exportVO.setSkuCode(item.getSkuCode());
            }
            if (StringUtils.hasText(item.getMainImage())) {
                try {
                    exportVO.setSkuImage(new java.net.URL(item.getMainImage()));
                } catch (java.net.MalformedURLException e) {
                    log.debug("Failed to parse SKU image URL: {}", item.getMainImage());
                }
            }
        }

        // 订单详情
        exportVO.setFulfillmentType(vo.getFulfillmentType());

        // 状态信息
        exportVO.setErpStatus(vo.getErpStatus());
        exportVO.setPlatformSubstatus(vo.getPlatformSubstatus());
        exportVO.setPlatformStatus(vo.getPlatformStatus());
        exportVO.setReconciliationStatus(vo.getReconciliationStatus());

        // 财务汇总信息
        exportVO.setFinancialRecordCount(vo.getFinancialRecordCount());
        exportVO.setSaleAmount(vo.getSaleAmount());
        exportVO.setActualIncome(vo.getActualIncome());
        exportVO.setFinancialCurrencyName(vo.getFinancialCurrencyName());

        // 金额与时间
        exportVO.setOrderTimeMoscow(vo.getOrderTimeMoscow());
        exportVO.setTotalAmountRub(vo.getTotalAmountRub());
        exportVO.setConvertedAmount(vo.getConvertedAmount());

        return exportVO;
    }



    // ==================== 私有辅助方法 ====================

    /**
     * 将 ErpOrderItem 列表转换为 OrderItemVO 列表，填充 SKU 信息
     */
    private List<OrderItemVO> buildOrderItemVOs(
            List<ErpOrderItem> items,
            Map<String, SkuBriefVO> skuBriefMap,
            Map<String, String> skuCodeMap) {
        return items.stream().map(item -> {
            OrderItemVO vo = new OrderItemVO();
            vo.setPlatformItemId(item.getPlatformItemId());
            String skuCode = skuCodeMap.get(item.getPlatformItemId());
            vo.setSkuCode(skuCode);
            vo.setQuantity(item.getQuantity());
            vo.setItemPrice(item.getItemPrice());
            vo.setItemAmount(item.getItemAmount());
            if (StringUtils.hasText(skuCode)) {
                SkuBriefVO brief = skuBriefMap.get(skuCode);
                if (brief != null) {
                    vo.setSkuName(brief.getSkuName());
                    vo.setMainImage(brief.getMainImage());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 从 rawJson 中提取 rid
     */
    private String extractRidFromRawJson(String rawJson) {
        if (rawJson == null || rawJson.isEmpty()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(rawJson);
            JsonNode ridNode = node.get("rid");
            if (ridNode != null && !ridNode.isNull()) {
                return ridNode.asText();
            }
        } catch (Exception e) {
            log.debug("Failed to extract rid from rawJson: {}", e.getMessage());
        }
        return null;
    }

    // 销售类操作名称
    private static final Set<String> SALE_OPER_NAMES = new HashSet<>(Arrays.asList("销售", "合规销售", "Продажа"));
    // 退货类操作名称
    private static final Set<String> RETURN_OPER_NAMES = new HashSet<>(Arrays.asList("退货", "Возврат", "销售冲销"));

    /**
     * 计算财务汇总
     * <p>
     * 重要说明：ppvz_for_pay 永远是正数或零，不区分正负方向
     * - 销售类：+ppvz_for_pay（卖家应收）
     * - 退货类：-ppvz_for_pay（需要扣回）
     * - 罚款/扣款：单独字段，需要额外扣除
     */
    private OrderReconciliationDetailVO.FinancialSummary calculateFinancialSummary(
            List<OrderReconciliationDetailVO.FinancialRecordItemVO> records) {
        
        OrderReconciliationDetailVO.FinancialSummary summary = new OrderReconciliationDetailVO.FinancialSummary();
        
        if (records == null || records.isEmpty()) {
            summary.setRecordCount(0);
            summary.setSaleRecordCount(0);
            summary.setReturnRecordCount(0);
            summary.setSaleAmount(BigDecimal.ZERO);
            summary.setReturnAmount(BigDecimal.ZERO);
            summary.setPenaltyAmount(BigDecimal.ZERO);
            summary.setDeductionAmount(BigDecimal.ZERO);
            summary.setActualIncome(BigDecimal.ZERO);
            return summary;
        }

        int saleCount = 0, returnCount = 0;
        BigDecimal saleAmount = BigDecimal.ZERO;
        BigDecimal returnAmount = BigDecimal.ZERO;
        BigDecimal penaltyAmount = BigDecimal.ZERO;
        BigDecimal deductionAmount = BigDecimal.ZERO;
        String currencyName = null;

        for (OrderReconciliationDetailVO.FinancialRecordItemVO record : records) {
            String operName = record.getSupplierOperName();
            BigDecimal ppvz = record.getPpvzForPay() != null ? record.getPpvzForPay() : BigDecimal.ZERO;
            BigDecimal penalty = record.getPenalty() != null ? record.getPenalty() : BigDecimal.ZERO;
            BigDecimal deduction = record.getDeduction() != null ? record.getDeduction() : BigDecimal.ZERO;

            // 累加罚款和扣款
            penaltyAmount = penaltyAmount.add(penalty);
            deductionAmount = deductionAmount.add(deduction);

            // 根据 supplier_oper_name 判断类型
            if (SALE_OPER_NAMES.contains(operName)) {
                saleCount++;
                saleAmount = saleAmount.add(ppvz);
            } else if (RETURN_OPER_NAMES.contains(operName)) {
                returnCount++;
                returnAmount = returnAmount.add(ppvz);
            }

            // 获取币种（取第一个非空的）
            if (currencyName == null && record.getCurrencyName() != null) {
                currencyName = record.getCurrencyName();
            }
        }

        // 实际收入 = 销售 - 退货 - 罚款 - 扣款
        BigDecimal actualIncome = saleAmount
                .subtract(returnAmount)
                .subtract(penaltyAmount)
                .subtract(deductionAmount);

        summary.setRecordCount(records.size());
        summary.setSaleRecordCount(saleCount);
        summary.setReturnRecordCount(returnCount);
        summary.setSaleAmount(saleAmount);
        summary.setReturnAmount(returnAmount);
        summary.setPenaltyAmount(penaltyAmount);
        summary.setDeductionAmount(deductionAmount);
        summary.setActualIncome(actualIncome);
        summary.setCurrencyName(currencyName);

        return summary;
    }

    /**
     * 判断订单对账状态
     * 注意：此方法用于详情查询，实际列表/导出的状态由 Mapper SQL 计算
     */
    private String determineReconciliationStatus(ErpOrder order,
                                                  List<OrderReconciliationDetailVO.FinancialRecordItemVO> financialRecords) {
        String erpStatus = order.getErpStatus();

        // 1. 待履约
        if (ErpOrderStatusEnum.READY_TO_SHIP.name().equals(erpStatus)) {
            return ReconciliationStatus.PENDING.name();
        }

        // 2. 运输中
        if (ErpOrderStatusEnum.SHIPPED.name().equals(erpStatus) || ErpOrderStatusEnum.ARRIVED_AT_PLATFORM_WAREHOUSE.name().equals(erpStatus)) {
            return ReconciliationStatus.IN_TRANSIT.name();
        }

        // 3. 已取消
        if (ErpOrderStatusEnum.CANCELED.name().equals(erpStatus)) {
            return ReconciliationStatus.CANCELED.name();
        }

        // 4. 已交付（必须有销售记录）
        if (ErpOrderStatusEnum.DELIVERED.name().equals(erpStatus)) {
            if (financialRecords != null && !financialRecords.isEmpty()) {
                boolean hasSale = financialRecords.stream()
                        .anyMatch(r -> SALE_OPER_NAMES.contains(r.getSupplierOperName()));
                if (hasSale) {
                    return ReconciliationStatus.MATCHED.name();
                }
            }
            return ReconciliationStatus.ANOMALY.name();
        }

        // 5. 已退货（有财务记录即可）
        if (ErpOrderStatusEnum.RETURNED.name().equals(erpStatus)) {
            if (financialRecords != null && !financialRecords.isEmpty()) {
                return ReconciliationStatus.MATCHED.name();
            }
            return ReconciliationStatus.ANOMALY.name();
        }

        // 6. 其他情况（兜底）
        return ReconciliationStatus.ANOMALY.name();
    }

}
