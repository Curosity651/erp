package com.erp.admin.order.service.label.strategy;

import java.util.Base64;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.enums.FulfillmentType;
import com.erp.admin.order.service.LabelConstants;
import com.erp.admin.order.service.LabelService;
import com.erp.admin.order.service.label.LabelPdfGenerator;
import com.erp.admin.order.service.label.LabelPrintStrategy;
import com.erp.admin.order.service.label.OrderGrouper;
import com.erp.admin.order.service.label.model.OrderFailureInfo;
import com.erp.admin.order.service.label.model.PdfGenerationRequest;
import com.erp.admin.order.service.label.model.PlatformContext;
import com.erp.admin.order.service.ozon.OzonPlatformApi;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.platform.ozon.credential.OzonCredential;
import com.erp.admin.platform.ozon.model.response.posting.OzonBarcodes;
import com.erp.admin.platform.ozon.model.response.posting.OzonPosting;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Ozon 面单打印策略
 * <p>
 * 实现 Ozon 平台特有的面单打印逻辑：
 * <ul>
 *   <li>面单来源：订单面单（PDF 格式）</li>
 *   <li>同步方式：从 Ozon API 获取 PDF 面单</li>
 *   <li>PDF 生成：合并多个订单 PDF 为一个文件</li>
 *   <li>校验规则：必须是 FBS 订单 + SKU 映射存在</li>
 *   <li>仓库名称：从订单 rawJson 中解析</li>
 * </ul>
 *
 * @author system
 * @see LabelPrintStrategy
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OzonLabelPrintStrategy implements LabelPrintStrategy {

    // ========== 依赖组件 ==========

    private final OzonPlatformApi ozonPlatformApi;
    private final ShopService shopService;
    private final LabelPdfGenerator pdfGenerator;
    private final LabelService labelService;
	private final CredentialService credentialService;

    // ========== 策略接口实现 ==========

    @Override
    public String getPlatformCode() {
        return PlatformEnum.Ozon.code();
    }

    /** PlatformContext 键：面单获取失败原因（orderId → 原因文案） */
    static final String CTX_LABEL_FAIL_REASONS = "labelFailReasons";

    @Override
    public PlatformContext syncLabels(List<ErpOrder> orders) {
        log.info("[OZON][LABEL] 开始同步面单: orderCount={}", orders.size());

        // 筛选缺失面单的订单，按店铺分组同步
        Map<Long, List<ErpOrder>> ordersByShop = orders.stream()
                .filter(order -> !StringUtils.hasText(order.getLabelBase64())
                        || !StringUtils.hasText(order.getLabelVerifyCodes()))
                .collect(Collectors.groupingBy(ErpOrder::getShopId));

        Map<Long, String> failReasons = new HashMap<>();
        for (Map.Entry<Long, List<ErpOrder>> entry : ordersByShop.entrySet()) {
            syncShopLabels(entry.getKey(), entry.getValue(), failReasons);
        }

        return PlatformContext.of(CTX_LABEL_FAIL_REASONS, failReasons);
    }

    @Override
    public OrderFailureInfo checkPrintability(ErpOrder order,
                                               Map<String, Sku> skuMap,
                                               Map<String, String> skuCodeMap,
                                               PlatformContext context) {
        // 规则1: 必须是 FBS 订单（FBO 由平台发货，无需打印面单）
        if (!FulfillmentType.FBS.matches(order.getFulfillmentType())) {
            return new OrderFailureInfo(order,
                    LabelConstants.ITEM_FAIL_ORDER_LABEL_MISSING,
                    "FBO 订单不支持打印面单");
        }

        // 规则2: 每个商品必须有 SKU 映射
        if (order.getItems() == null || order.getItems().isEmpty()
                || order.getItems().stream()
                       .anyMatch(item -> !skuCodeMap.containsKey(item.getPlatformItemId()))) {
            return new OrderFailureInfo(order,
                    LabelConstants.ITEM_FAIL_SKU_MAPPING_MISSING,
                    "SKU 映射缺失");
        }

        // 规则3: 面单必须已缓存（同步阶段获取失败的订单不能静默缺失于 PDF，需带原因报告失败）
        if (!StringUtils.hasText(order.getLabelBase64())) {
            String reason = null;
            if (context != null) {
                Map<Long, String> failReasons = context.get(CTX_LABEL_FAIL_REASONS);
                if (failReasons != null) {
                    reason = failReasons.get(order.getId());
                }
            }
            return new OrderFailureInfo(order,
                    LabelConstants.ITEM_FAIL_ORDER_LABEL_MISSING,
                    reason != null ? reason : "面单未获取");
        }

        return null;
    }

    @Override
    public void enrichWarehouseNames(Map<String, OrderGrouper.OrderGroup> groups) {
        for (OrderGrouper.OrderGroup group : groups.values()) {
            if (group.getOrders().isEmpty()) {
                continue;
            }

            // 从分组第一个订单的 rawJson 中提取仓库名称
            ErpOrder firstOrder = group.getOrders().get(0);
            String warehouseName = extractWarehouseNameFromRawJson(firstOrder);
            group.setDestinationWarehouseName(warehouseName);
        }
    }

    @Override
    public void generatePdfFiles(PdfGenerationRequest request) {
        // Ozon: 合并所有订单 PDF 为一个文件
        for (Map.Entry<String, OrderGrouper.OrderGroup> entry : request.getGroups().entrySet()) {
            LabelPdfGenerator.PdfGenerationContext context = new LabelPdfGenerator.PdfGenerationContext(
                    request.getBatchId(),
                    request.getBatchNo(),
                    getPlatformCode(),
                    entry.getKey(),
                    entry.getValue(),
                    "OZON",
                    LabelConstants.FILE_TYPE_OZON_PDF
            );

            // 收集订单 PDF 并合并
            pdfGenerator.mergePdfs(context, orders -> orders.stream()
                    .map(ErpOrder::getLabelBase64)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList()));
        }
    }

    // ========== 私有方法：面单同步 ==========

    /**
     * 同步单个店铺的订单面单
     *
     * @param shopId      店铺 ID
     * @param orders      该店铺下缺失面单的订单列表
     * @param failReasons 失败原因收集器（orderId → 原因文案）
     */
    private void syncShopLabels(Long shopId, List<ErpOrder> orders, Map<Long, String> failReasons) {
        try {
            // 获取店铺凭证
            Shop shop = shopService.getById(shopId);
            if (shop == null) {
                log.error("[OZON][LABEL] 店铺不存在: shopId={}", shopId);
                orders.forEach(o -> failReasons.put(o.getId(), "店铺不存在"));
                return;
            }
            OzonCredential credential = credentialService.parseCredential(shop);

            // 逐单获取面单（避免官方批量接口"一单未就绪整批失败"的语义）
            for (ErpOrder order : orders) {
                syncSingleOrderLabel(order, credential, shopId, failReasons);
            }

        } catch (Exception e) {
            log.error("[OZON][LABEL] 同步店铺面单失败: shopId={}, error={}",
                    shopId, e.getMessage(), e);
            orders.stream()
                    .filter(o -> !StringUtils.hasText(o.getLabelBase64()))
                    .forEach(o -> failReasons.putIfAbsent(o.getId(), "面单获取失败: " + e.getMessage()));
        }
    }

    /**
     * 同步单个订单的面单
     *
     * @param order       订单
     * @param credential  Ozon 凭证
     * @param shopId      店铺 ID（用于日志）
     * @param failReasons 失败原因收集器
     */
    private void syncSingleOrderLabel(ErpOrder order, OzonCredential credential, Long shopId,
                                       Map<Long, String> failReasons) {
        try {
            String postingNumber = order.getShipmentId();
            if (!StringUtils.hasText(postingNumber)) {
                log.warn("[OZON][LABEL] 订单缺少发货单号: orderId={}", order.getId());
                failReasons.put(order.getId(), "订单缺少发货单号");
                return;
            }

            // 从 Ozon API 获取面单 PDF
            String labelBase64 = order.getLabelBase64();
            if (!StringUtils.hasText(labelBase64)) {
                byte[] fileContent = ozonPlatformApi.getPackageLabel(credential, postingNumber);
                if (fileContent == null || fileContent.length == 0) {
                    log.warn("[OZON][LABEL] 获取面单失败，返回数据为空: orderId={}", order.getId());
                    failReasons.put(order.getId(), "面单未获取（平台返回为空）");
                    return;
                }
                labelBase64 = Base64.getEncoder().encodeToString(fileContent);
            }

            OzonPosting posting = ozonPlatformApi
                    .batchFetchPostings(credential, java.util.Collections.singletonList(postingNumber))
                    .get(postingNumber);
            labelService.updateOrderLabel(order.getId(), labelBase64,
                    collectVerifyCodes(order, posting));

            // 更新内存中的订单对象，供后续流程使用
            order.setLabelBase64(labelBase64);

            log.info("[OZON][LABEL] 更新面单成功: shopId={}, orderId={}", shopId, order.getId());

        } catch (Exception e) {
            failReasons.put(order.getId(), resolveLabelFailReason(e));
            log.error("[OZON][LABEL] 同步面单失败: orderId={}, error={}",
                    order.getId(), e.getMessage(), e);
        }
    }

    private List<String> collectVerifyCodes(ErpOrder order, OzonPosting posting) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        if (StringUtils.hasText(order.getPlatformOrderId())) {
            codes.add(order.getPlatformOrderId().trim());
        }
        if (StringUtils.hasText(order.getShipmentId())) {
            codes.add(order.getShipmentId().trim());
        }
        if (posting != null) {
            if (StringUtils.hasText(posting.getPostingNumber())) {
                codes.add(posting.getPostingNumber().trim());
            }
            if (StringUtils.hasText(posting.getOrderNumber())) {
                codes.add(posting.getOrderNumber().trim());
            }
            OzonBarcodes barcodes = posting.getBarcodes();
            if (barcodes != null) {
                if (StringUtils.hasText(barcodes.getUpperBarcode())) {
                    codes.add(barcodes.getUpperBarcode().trim());
                }
                if (StringUtils.hasText(barcodes.getLowerBarcode())) {
                    codes.add(barcodes.getLowerBarcode().trim());
                }
            }
        }
        return new ArrayList<>(codes);
    }

    /**
     * 将面单获取异常翻译成运营可理解的原因。
     * <p>
     * Ozon 官方：面单在确认发货（ship）后约 45-60 秒生成，未就绪时报
     * "The next postings aren't ready"，稍后重试即可（重点一次打印按钮即自动补拉）。
     */
    private String resolveLabelFailReason(Exception e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        String lower = msg.toLowerCase();
        if (lower.contains("aren't ready") || lower.contains("not ready")
                || lower.contains("postings_not_ready")) {
            return "面单生成中（确认发货后平台约需1分钟），请稍后重新打印";
        }
        return "面单获取失败: " + msg;
    }

    // ========== 私有方法：仓库名称解析 ==========

    /**
     * 从订单的 rawJson 中提取仓库名称
     * <p>
     * Ozon 订单的仓库信息存储在 rawJson.delivery_method.name 字段中。
     *
     * @param order 订单
     * @return 仓库名称，解析失败时返回占位符
     */
    private String extractWarehouseNameFromRawJson(ErpOrder order) {
        if (!StringUtils.hasText(order.getRawJson())) {
            return "{warehouseName}";
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(order.getRawJson());
            JsonNode deliveryMethod = root.path("delivery_method");

            if (deliveryMethod.has("name")) {
                return deliveryMethod.get("name").asText();
            }
        } catch (Exception e) {
            log.warn("[OZON][LABEL] 解析仓库名称失败: orderId={}, error={}",
                    order.getId(), e.getMessage());
        }

        return "{warehouseName}";
    }
}
