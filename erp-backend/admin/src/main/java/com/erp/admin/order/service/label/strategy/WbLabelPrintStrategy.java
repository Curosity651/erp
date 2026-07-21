package com.erp.admin.order.service.label.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.admin.order.mapper.WbOfficeMapper;
import com.erp.admin.order.mapper.WbSupplyMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.WbOffice;
import com.erp.admin.order.model.entity.WbSupply;
import com.erp.admin.order.service.LabelConstants;
import com.erp.admin.order.service.label.LabelPdfGenerator;
import com.erp.admin.order.service.label.LabelPrintStrategy;
import com.erp.admin.order.service.label.OrderGrouper;
import com.erp.admin.order.service.label.model.OrderFailureInfo;
import com.erp.admin.order.service.label.model.PdfGenerationRequest;
import com.erp.admin.order.service.label.model.PlatformContext;
import com.erp.admin.order.service.wildberries.sync.WbLabelSyncService;
import com.erp.admin.order.service.wildberries.sync.WbSupplySyncService;
import com.erp.admin.order.util.LabelUtils;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.platform.wildberries.WildberriesClient;
import com.erp.admin.platform.wildberries.credential.WbCredential;
import com.erp.admin.platform.wildberries.model.request.sticker.WbGetStickersRequest;
import com.erp.admin.platform.wildberries.model.response.sticker.WbStickersResponse;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * WB（Wildberries）面单打印策略
 * <p>
 * 实现 WB 平台特有的面单打印逻辑：
 * <ul>
 *   <li>面单来源：订单面单（PNG）+ Supply 批次面单（PNG）</li>
 *   <li>同步方式：批量获取订单面单 + 逐个同步 Supply 面单</li>
 *   <li>PDF 生成：图片转 PDF，生成 2 个文件（订单面单 + Supply 面单）</li>
 *   <li>校验规则：shipmentId + SKU 映射 + 订单面单 + Supply 面单</li>
 *   <li>仓库名称：查询 WbOffice 表</li>
 * </ul>
 *
 * @author system
 * @see LabelPrintStrategy
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WbLabelPrintStrategy implements LabelPrintStrategy {

    // ========== 依赖组件 ==========

    private final WildberriesClient wbClient;
	private final ShopService shopService;
	private final CredentialService credentialService;
    private final WbSupplyMapper wbSupplyMapper;
    private final WbOfficeMapper wbOfficeMapper;
    private final WbLabelSyncService wbLabelSyncService;
    private final WbSupplySyncService wbSupplySyncService;
    private final LabelPdfGenerator pdfGenerator;

    // ========== 常量 ==========

    /**
     * 平台上下文中 SupplyMap 的键名
     */
    private static final String CTX_SUPPLY_MAP = "supplyMap";

    // ========== 策略接口实现 ==========

    @Override
    public String getPlatformCode() {
        return PlatformEnum.Wildberries.code();
    }

    @Override
    public PlatformContext syncLabels(List<ErpOrder> orders) {
        log.info("[WB][LABEL] 开始同步面单: orderCount={}", orders.size());

        // 1. 预加载 Supply 数据
        Map<String, WbSupply> supplyMap = loadSupplies(orders);

        // 2. 按店铺分组同步面单
        Map<Long, List<ErpOrder>> ordersByShop = orders.stream()
                .collect(Collectors.groupingBy(ErpOrder::getShopId));

        for (Map.Entry<Long, List<ErpOrder>> entry : ordersByShop.entrySet()) {
            syncShopLabels(entry.getKey(), entry.getValue(), supplyMap);
        }

        // 3. 返回 SupplyMap 作为上下文，供后续步骤使用
        return PlatformContext.of(CTX_SUPPLY_MAP, supplyMap);
    }

    @Override
    public OrderFailureInfo checkPrintability(ErpOrder order,
                                               Map<String, Sku> skuMap,
                                               Map<String, String> skuCodeMap,
                                               PlatformContext context) {
        // 规则1: 必须有 shipmentId（批次号）
        if (!StringUtils.hasText(order.getShipmentId())) {
            return new OrderFailureInfo(order,
                    LabelConstants.ITEM_FAIL_SHIPMENT_MISSING,
                    "缺少批次号(shipmentId)");
        }

        // 规则2: 每个商品必须有 SKU 映射
        if (order.getItems() == null || order.getItems().isEmpty()
                || order.getItems().stream()
                       .anyMatch(item -> !skuCodeMap.containsKey(item.getPlatformItemId()))) {
            return new OrderFailureInfo(order,
                    LabelConstants.ITEM_FAIL_SKU_MAPPING_MISSING,
                    "SKU 映射缺失");
        }

        // 规则3: 订单面单必须存在
        if (!StringUtils.hasText(order.getLabelBase64())) {
            return new OrderFailureInfo(order,
                    LabelConstants.ITEM_FAIL_ORDER_LABEL_MISSING,
                    "订单面单缺失");
        }

        // 规则4: Supply 记录必须存在
        Map<String, WbSupply> supplyMap = context.get(CTX_SUPPLY_MAP);
        WbSupply supply = supplyMap.get(order.getShipmentId());
        if (supply == null) {
            return new OrderFailureInfo(order,
                    LabelConstants.ITEM_FAIL_SUPPLY_NOT_FOUND,
                    "批次信息不存在");
        }

        // 规则5: Supply 面单必须存在
        if (!StringUtils.hasText(supply.getLabelBase64())) {
            return new OrderFailureInfo(order,
                    LabelConstants.ITEM_FAIL_SUPPLY_LABEL_MISSING,
                    "批次面单缺失");
        }

        // 所有规则通过，可打印
        return null;
    }

    @Override
    public void enrichWarehouseNames(Map<String, OrderGrouper.OrderGroup> groups) {
        // 收集所有仓库 ID
        Set<Long> officeIds = new LinkedHashSet<>();
        for (OrderGrouper.OrderGroup group : groups.values()) {
            if (group.getDestinationWarehouseId() != null) {
                try {
                    officeIds.add(Long.parseLong(group.getDestinationWarehouseId()));
                } catch (NumberFormatException ignore) {
                    // 非数字格式的仓库 ID，跳过
                }
            }
        }

        if (officeIds.isEmpty()) {
            return;
        }

        // 批量查询仓库名称
        List<WbOffice> offices = wbOfficeMapper.selectByOfficeIds(officeIds);
        Map<String, String> idToName = new HashMap<>();
        for (WbOffice office : offices) {
            idToName.put(String.valueOf(office.getOfficeId()), office.getName());
        }

        // 填充仓库名称
        for (OrderGrouper.OrderGroup group : groups.values()) {
            String name = idToName.get(group.getDestinationWarehouseId());
            group.setDestinationWarehouseName(name != null ? name : "{officeName}");
        }
    }

    @Override
    public void generatePdfFiles(PdfGenerationRequest request) {
        Map<String, WbSupply> supplyMap = request.getContext().get(CTX_SUPPLY_MAP);

        // WB: 为每个分组生成 2 个 PDF 文件
        for (Map.Entry<String, OrderGrouper.OrderGroup> entry : request.getGroups().entrySet()) {
            String groupKey = entry.getKey();
            OrderGrouper.OrderGroup group = entry.getValue();

            // 1. 生成订单面单 PDF（多张图片合并为一个 PDF）
            generateOrderPdf(request, groupKey, group);

            // 2. 生成 Supply 面单 PDF（去重后的批次面单）
            generateSupplyPdf(request, groupKey, group, supplyMap);
        }
    }

    // ========== 私有方法：数据加载 ==========

    /**
     * 预加载订单关联的 Supply 数据
     *
     * @param orders 订单列表
     * @return supplyId -> WbSupply 映射
     */
    private Map<String, WbSupply> loadSupplies(List<ErpOrder> orders) {
        Set<String> supplyIds = orders.stream()
                .map(ErpOrder::getShipmentId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (supplyIds.isEmpty()) {
            return new HashMap<>();
        }

        List<WbSupply> supplies = wbSupplyMapper.selectByPlatformAndSupplyIds(
                getPlatformCode(), supplyIds);

        Map<String, WbSupply> map = new HashMap<>();
        if (supplies != null) {
            for (WbSupply supply : supplies) {
                if (supply != null && StringUtils.hasText(supply.getSupplyId())) {
                    map.put(supply.getSupplyId(), supply);
                }
            }
        }
        return map;
    }

    // ========== 私有方法：面单同步 ==========

    /**
     * 同步单个店铺的面单（订单面单 + Supply 面单）
     *
     * @param shopId    店铺 ID
     * @param orders    该店铺的订单列表
     * @param supplyMap Supply 映射（会被更新）
     */
    private void syncShopLabels(Long shopId, List<ErpOrder> orders,
                                 Map<String, WbSupply> supplyMap) {
        try {
			Shop shop = shopService.getById(shopId);
			WbCredential credential = credentialService.parseCredential(shop);

            // 1. 批量获取缺失的订单面单
            Map<Long, String> orderIdToLabel = fetchMissingOrderLabels(orders, credential, shopId);

            // 2. 逐单处理
            for (ErpOrder order : orders) {
                // 2.1 同步订单面单
                syncOrderLabel(order, orderIdToLabel, credential);

                // 2.2 同步 Supply 面单
                syncSupplyLabel(order, supplyMap, credential, shopId);
            }

        } catch (Exception e) {
            log.error("[WB][LABEL] 同步店铺面单失败: shopId={}, error={}",
                    shopId, e.getMessage(), e);
        }
    }

    /**
     * 批量获取缺失的订单面单
     */
    private Map<Long, String> fetchMissingOrderLabels(List<ErpOrder> orders,
                                                        WbCredential credential,
                                                        Long shopId) {
        List<Long> missingOrderIds = new ArrayList<>();
        for (ErpOrder order : orders) {
            if (!StringUtils.hasText(order.getLabelBase64())) {
                Long wbOrderId = LabelUtils.toLong(order.getPlatformOrderId());
                if (wbOrderId != null) {
                    missingOrderIds.add(wbOrderId);
                }
            }
        }

        if (missingOrderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, String> result = batchFetchOrderLabelsFromPlatform(credential, missingOrderIds);
        log.info("[WB][LABEL] 批量获取订单面单: shopId={}, count={}", shopId, result.size());
        return result;
    }

    /**
     * 同步单个订单的面单
     */
    private void syncOrderLabel(ErpOrder order, Map<Long, String> orderIdToLabel,
                                 WbCredential credential) {
        if (StringUtils.hasText(order.getLabelBase64())) {
            return;
        }

        try {
            String labelFromBatch = orderIdToLabel.get(LabelUtils.toLong(order.getPlatformOrderId()));
            wbLabelSyncService.syncOrderLabel(order, labelFromBatch, credential);
        } catch (Exception e) {
            log.error("[WB][LABEL] 订单面单同步失败: orderId={}, error={}",
                    order.getId(), e.getMessage(), e);
        }
    }

    /**
     * 同步 Supply 面单
     */
    private void syncSupplyLabel(ErpOrder order, Map<String, WbSupply> supplyMap,
                                  WbCredential credential, Long shopId) {
        String supplyId = order.getShipmentId();
        if (!StringUtils.hasText(supplyId)) {
            return;
        }

        WbSupply supply = supplyMap.get(supplyId);

        // 如果 Supply 已有面单，跳过
        if (supply != null && StringUtils.hasText(supply.getLabelBase64())) {
            return;
        }

        // 如果 Supply 不存在，先创建基础数据
        if (supply == null) {
            try {
                supply = wbSupplySyncService.syncSupplyBasicData(shopId, supplyId, credential);
                supplyMap.put(supplyId, supply);
                log.info("[WB][SUPPLY] Supply 基础数据已保存: supplyId={}", supplyId);
            } catch (Exception e) {
                log.error("[WB][SUPPLY] Supply 基础数据同步失败: supplyId={}, error={}",
                        supplyId, e.getMessage(), e);
                return;
            }
        }

        // 同步 Supply 面单
        try {
            wbSupplySyncService.syncSupplyLabel(supply, credential);
        } catch (Exception e) {
            log.warn("[WB][LABEL] Supply 面单同步失败，基础数据已保存: supplyId={}, error={}",
                    supplyId, e.getMessage());
        }
    }

    /**
     * 从 WB 平台批量获取订单面单
     */
    private Map<Long, String> batchFetchOrderLabelsFromPlatform(WbCredential credential,
                                                                  List<Long> platformOrderIds) {
        if (platformOrderIds == null || platformOrderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            WbGetStickersRequest request = WbGetStickersRequest.builder()
                    .orders(platformOrderIds)
                    .build();

            WbStickersResponse response = wbClient.getOrderStickersTyped(credential, request);

            return LabelUtils.parseOrderStickersTyped(response);
        } catch (Exception ex) {
            log.warn("[WB][LABEL] 批量获取订单面单失败: size={}, error={}",
                    platformOrderIds.size(), ex.getMessage());
            return Collections.emptyMap();
        }
    }

    // ========== 私有方法：PDF 生成 ==========

    /**
     * 生成订单面单 PDF
     * <p>
     * 将分组内所有订单的面单图片合并为一个 PDF 文件。
     */
    private void generateOrderPdf(PdfGenerationRequest request,
                                   String groupKey, OrderGrouper.OrderGroup group) {
        LabelPdfGenerator.PdfGenerationContext context = new LabelPdfGenerator.PdfGenerationContext(
                request.getBatchId(),
                request.getBatchNo(),
                getPlatformCode(),
                groupKey,
                group,
                LabelConstants.FILE_NAME_ORDER,
                LabelConstants.FILE_TYPE_WB_ORDER_PDF
        );

        pdfGenerator.generateImagesPdf(context, orders -> orders.stream()
                .map(ErpOrder::getLabelBase64)
                .filter(labelBase64 -> StringUtils.hasText(labelBase64))
                .collect(Collectors.toList()));
    }

    /**
     * 生成 Supply 面单 PDF
     * <p>
     * 将分组内所有订单关联的 Supply 面单（去重）合并为一个 PDF 文件。
     */
    private void generateSupplyPdf(PdfGenerationRequest request,
                                    String groupKey, OrderGrouper.OrderGroup group,
                                    Map<String, WbSupply> supplyMap) {
        LabelPdfGenerator.PdfGenerationContext context = new LabelPdfGenerator.PdfGenerationContext(
                request.getBatchId(),
                request.getBatchNo(),
                getPlatformCode(),
                groupKey,
                group,
                LabelConstants.FILE_NAME_SUPPLY,
                LabelConstants.FILE_TYPE_WB_SUPPLY_PDF
        );

        pdfGenerator.generateImagesPdf(context, orders -> {
            // 收集唯一的 Supply ID（保持顺序）
            Set<String> uniqueSupplyIds = orders.stream()
                    .map(ErpOrder::getShipmentId)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            // 获取对应的面单图片
            return uniqueSupplyIds.stream()
                    .map(supplyMap::get)
                    .filter(supply -> supply != null && StringUtils.hasText(supply.getLabelBase64()))
                    .map(WbSupply::getLabelBase64)
                    .collect(Collectors.toList());
        });
    }
}
