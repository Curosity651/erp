package com.erp.admin.order.service.label;

import com.erp.admin.order.model.entity.ErpLabelBatch;
import com.erp.admin.order.model.entity.ErpLabelBatchItem;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.ErpOrderItem;
import com.erp.admin.order.model.vo.LabelBatchVO;
import com.erp.admin.order.service.LabelService;
import com.erp.admin.order.service.label.model.OrderFailureInfo;
import com.erp.admin.order.service.label.model.PdfGenerationRequest;
import com.erp.admin.order.service.label.model.PlatformContext;
import com.erp.admin.product.model.entity.Sku;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

/**
 * 面单打印统一编排器
 * <p>
 * 定义面单打印的标准流程骨架（11步），通过策略模式处理平台差异。
 * <p>
 * 流程步骤：
 * <ol>
 *   <li>加载订单</li>
 *   <li>验证订单未锁定</li>
 *   <li>同步面单（策略）</li>
 *   <li>加载 SKU 映射</li>
 *   <li>分类订单（策略判断）</li>
 *   <li>分组订单</li>
 *   <li>丰富仓库名称（策略）</li>
 *   <li>创建批次和批次项</li>
 *   <li>标记失败订单</li>
 *   <li>生成 PDF（策略）</li>
 *   <li>更新统计并返回</li>
 * </ol>
 * <p>
 * 扩展新平台只需实现 {@link LabelPrintStrategy} 接口，无需修改此编排器。
 *
 * @author system
 * @see LabelPrintStrategy
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LabelPrintOrchestrator {

    // ========== 通用组件 ==========

    private final OrderDataLoader orderDataLoader;
    private final OrderGrouper orderGrouper;
    private final LabelService labelService;

    // ========== 策略注册表 ==========

    /**
     * 平台策略映射（platformCode -> Strategy）
     * 由 {@link LabelPrintConfig} 自动注入
     */
	@Resource(name = "labelPrintStrategyMap")
    private final Map<String, LabelPrintStrategy> labelPrintStrategyMap;

    // ========== 公开方法 ==========

    /**
     * 统一打印入口
     * <p>
     * 根据平台代码路由到对应策略，执行标准化的打印流程。
     *
     * @param platform 平台代码
     * @param orderIds 订单 ID 列表
     * @param userId   操作用户 ID
     * @param remark   备注
     * @return 批次信息
     * @throws IllegalArgumentException 平台不支持或订单无效
     */
    public LabelBatchVO printLabels(String platform,
                                     List<Long> orderIds,
                                     Long userId,
                                     String remark) {

        log.info("[{}][LABEL] 开始打印面单: orderIds={}, userId={}, remark={}",
                platform, orderIds, userId, remark);

        // 获取平台策略
        LabelPrintStrategy strategy = getStrategy(platform);

        // Step 1: 加载订单
        List<ErpOrder> orders = orderDataLoader.loadOrders(orderIds, platform);
        validateNotEmpty(orders, platform);

        // Step 2: 验证订单未锁定
        validateOrdersNotLocked(orders);

        // Step 3: 同步面单（策略实现）
        PlatformContext context = strategy.syncLabels(orders);

        // Step 4: 加载 SKU 映射（key = skuCode）
        Map<String, String> skuCodeMap = orderDataLoader.loadSkuCodeMap(orders);
        Map<String, Sku> skuMap = orderDataLoader.loadSkuMap(orders);

        // Step 5: 分类订单（编排器遍历，策略判断）
        ClassificationResult classification = classifyOrders(orders, skuMap, skuCodeMap, context, strategy);

        // Step 6: 分组（按仓库 + SKU）
        Map<String, OrderGrouper.OrderGroup> groups = orderGrouper.groupByWarehouseAndSku(
                classification.getPrintable(), skuMap, skuCodeMap);

        // Step 7: 丰富仓库名称（策略实现）
        strategy.enrichWarehouseNames(groups);

        // Step 8: 创建批次和批次项
        ErpLabelBatch batch = labelService.createBatch(platform, orders, remark, userId);
        createBatchItems(batch.getId(), orders, skuMap, skuCodeMap);

        // Step 9: 标记失败订单
        markFailedOrders(batch.getId(), classification.getFailed());

        // Step 10: 生成 PDF（策略实现）
        strategy.generatePdfFiles(new PdfGenerationRequest(
                batch.getId(), batch.getBatchNo(), platform, groups, context));

        // Step 11: 更新统计并返回
        labelService.updateBatchStatistics(batch.getId());

        log.info("[{}][LABEL] 打印面单完成: batchId={}, batchNo={}",
                platform, batch.getId(), batch.getBatchNo());

        return labelService.getBatchVO(batch.getId());
    }

    // ========== 私有方法：策略路由 ==========

    /**
     * 获取平台策略
     *
     * @param platform 平台代码
     * @return 对应的策略实现
     * @throws IllegalArgumentException 平台不支持
     */
    private LabelPrintStrategy getStrategy(String platform) {
        LabelPrintStrategy strategy = labelPrintStrategyMap.get(platform);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的平台: " + platform);
        }
        return strategy;
    }

    // ========== 私有方法：验证 ==========

    /**
     * 验证订单列表非空
     */
    private void validateNotEmpty(List<ErpOrder> orders, String platform) {
        if (orders == null || orders.isEmpty()) {
            throw new IllegalArgumentException("未找到有效的 " + platform + " 订单");
        }
    }

    /**
     * 验证订单未锁定
     * <p>
     * 锁定的订单不允许打印，避免并发操作冲突。
     */
    private void validateOrdersNotLocked(List<ErpOrder> orders) {
        for (ErpOrder order : orders) {
            if (order.getLocked() != null && order.getLocked() == 1) {
                throw new IllegalArgumentException("存在锁定订单，不允许打印");
            }
        }
    }

    // ========== 私有方法：分类 ==========

    /**
     * 分类订单
     * <p>
     * 遍历所有订单，调用策略的 checkPrintability 方法判断是否可打印。
     *
     * @param orders   订单列表
     * @param skuMap   SKU 映射
     * @param context  平台上下文
     * @param strategy 平台策略
     * @return 分类结果（可打印 + 失败）
     */
    private ClassificationResult classifyOrders(List<ErpOrder> orders,
                                                  Map<String, Sku> skuMap,
                                                  Map<String, String> skuCodeMap,
                                                  PlatformContext context,
                                                  LabelPrintStrategy strategy) {
        List<ErpOrder> printable = new ArrayList<>();
        List<OrderFailureInfo> failed = new ArrayList<>();

        for (ErpOrder order : orders) {
            OrderFailureInfo failure = strategy.checkPrintability(order, skuMap, skuCodeMap, context);

            if (failure == null) {
                printable.add(order);
            } else {
                failed.add(failure);
            }
        }

        return new ClassificationResult(printable, failed);
    }

    // ========== 私有方法：批次项 ==========

    /**
     * 创建批次项
     * <p>
     * 为每个订单创建对应的批次项记录，包含分组键、SKU 信息等。
     *
     * @param batchId 批次 ID
     * @param orders  订单列表
     * @param skuMap  SKU 映射
     */
    private void createBatchItems(Long batchId, List<ErpOrder> orders,
                                   Map<String, Sku> skuMap,
                                   Map<String, String> skuCodeMap) {
        List<ErpLabelBatchItem> items = new ArrayList<>();

        for (ErpOrder order : orders) {
            ErpLabelBatchItem item = new ErpLabelBatchItem();
            item.setBatchId(batchId);
            item.setShopId(order.getShopId());
            item.setOrderId(order.getId());
            item.setPlatformOrderId(order.getPlatformOrderId());
            item.setSupplyId(order.getShipmentId());
            item.setWarehouseId(order.getWarehouseId());
            item.setPlatform(order.getPlatform());

            // 从 items 获取数量和 SKU 信息
            List<ErpOrderItem> orderItems = order.getItems();
            item.setSkuQty(orderItems == null || orderItems.isEmpty()
                    ? 0
                    : orderItems.stream().mapToInt(ErpOrderItem::getQuantity).sum());

            String skuCode;
            String platformItemId = (orderItems != null && !orderItems.isEmpty())
                    ? orderItems.get(0).getPlatformItemId() : null;
            if (StringUtils.hasText(platformItemId)
                    && skuCodeMap.containsKey(platformItemId)) {
                skuCode = skuCodeMap.get(platformItemId);
            } else {
                skuCode = "";
            }
            String groupKey = orderGrouper.buildGroupKey(order.getDestinationWarehouseId(), skuCode);
            item.setGroupKey(groupKey);
            item.setErpSkuCode(skuCode);

            items.add(item);
        }

        labelService.createBatchItems(batchId, items);
    }

    // ========== 私有方法：失败标记 ==========

    /**
     * 标记失败订单
     * <p>
     * 将分类阶段收集的失败信息写入批次项记录。
     *
     * @param batchId  批次 ID
     * @param failures 失败信息列表
     */
    private void markFailedOrders(Long batchId, List<OrderFailureInfo> failures) {
        for (OrderFailureInfo failure : failures) {
            labelService.updateItemFailure(
                    batchId,
                    failure.getOrder().getId(),
                    failure.getFailureCode(),
                    failure.getErrorMessage());
        }
    }

    // ========== 内部类 ==========

    /**
     * 订单分类结果
     * <p>
     * 编排器内部使用，封装可打印订单和失败订单。
     */
    @Getter
    @RequiredArgsConstructor
    private static class ClassificationResult {
        private final List<ErpOrder> printable;
        private final List<OrderFailureInfo> failed;
    }
}
