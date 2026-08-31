package com.erp.admin.wms.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.groups.Default;

import com.erp.admin.order.mapper.ErpOrderItemMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.ErpOrderItem;
import com.erp.admin.order.model.qo.PendingOrderQO;
import com.erp.admin.order.service.ErpOrderService;
import com.erp.admin.product.model.vo.SkuBriefVO;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.product.service.SkuMappingService;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import com.erp.admin.wms.enums.WmsResultCode;
import com.erp.admin.wms.config.WmsCoreModeGuard;
import com.erp.admin.wms.facade.SalesOutboundFacade;
import com.erp.admin.wms.model.dto.SalesOutboundDTO;
import com.erp.admin.wms.model.qo.SalesOutboundQO;
import com.erp.admin.wms.model.vo.PendingOrderItemVO;
import com.erp.admin.wms.model.vo.PendingOrderVO;
import com.erp.admin.wms.model.vo.SalesOutboundDetailVO;
import com.erp.admin.wms.model.vo.SalesOutboundExportVO;
import com.erp.admin.wms.model.vo.SalesOutboundPageVO;
import com.erp.admin.wms.model.vo.StockShortageVO;
import com.erp.admin.wms.service.SalesOutboundService;
import com.erp.admin.wms.service.WmsPhysicalInventoryService;
import com.erp.admin.wms.model.enums.StockStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.validation.group.CreateGroup;
import org.ballcat.common.core.validation.group.UpdateGroup;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.fastexcel.annotation.ResponseExcel;
import org.ballcat.log.operation.annotation.OperationLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 销售出库单管理
 *
 * @author erp
 */
@RestController
@RequestMapping("/wms/sales-outbound")
@Tag(name = "销售出库单管理")
@RequiredArgsConstructor
public class SalesOutboundController {

    private final SalesOutboundFacade salesOutboundFacade;
    private final SalesOutboundService salesOutboundService;
    private final WmsPhysicalInventoryService physicalInventoryService;
    private final ErpOrderService erpOrderService;
    private final ShopService shopService;
    private final SkuBriefService skuBriefService;
    private final SkuMappingService skuMappingService;
    private final ErpOrderItemMapper orderItemMapper;
    private final WmsCoreModeGuard coreModeGuard;

    @GetMapping("/page")
    @PreAuthorize("@per.hasPermission('wms:sales-outbound:read')")
    @Operation(summary = "分页查询销售出库单")
    public ApiResult<PageResult<SalesOutboundPageVO>> getPage(PageParam pageParam, SalesOutboundQO qo) {
        return ApiResult.ok(salesOutboundService.queryPage(pageParam, qo));
    }

    @GetMapping("/detail")
    @PreAuthorize("@per.hasPermission('wms:sales-outbound:read')")
    @Operation(summary = "获取销售出库单详情")
    public ApiResult<SalesOutboundDetailVO> getDetail(@RequestParam Long id) {
        return ApiResult.ok(salesOutboundService.getDetail(id));
    }

    @PostMapping
    @PreAuthorize("@per.hasPermission('wms:sales-outbound:add')")
    @OperationLog(bizType = "销售出库单管理", successMessage = "新建成功")
    @Operation(summary = "新建销售出库单")
    public ApiResult<Long> create(@Validated({Default.class, CreateGroup.class}) @RequestBody SalesOutboundDTO dto) {
        coreModeGuard.assertLegacyWriteAllowed("旧销售出库创建");
        return ApiResult.ok(salesOutboundFacade.create(dto));
    }

    @PutMapping
    @PreAuthorize("@per.hasPermission('wms:sales-outbound:edit')")
    @OperationLog(bizType = "销售出库单管理", successMessage = "编辑成功")
    @Operation(summary = "编辑销售出库单")
    public ApiResult<Void> update(@Validated({Default.class, UpdateGroup.class}) @RequestBody SalesOutboundDTO dto) {
        coreModeGuard.assertLegacyWriteAllowed("旧销售出库编辑");
        salesOutboundFacade.update(dto);
        return ApiResult.ok();
    }

    @PatchMapping("/confirm")
    @PreAuthorize("@per.hasPermission('wms:sales-outbound:edit')")
    @OperationLog(bizType = "销售出库单管理", successMessage = "确认出库成功")
    @Operation(summary = "确认出库")
    public ApiResult<List<StockShortageVO>> confirm(@RequestParam Long id) {
        coreModeGuard.assertLegacyWriteAllowed("旧销售出库确认");
        List<StockShortageVO> shortages = salesOutboundFacade.confirm(id);
        if (!shortages.isEmpty()) {
            // 构建错误消息
            String skuList = shortages.stream()
                    .map(s -> s.getSkuName() + "(缺" + s.getShortage() + ")")
                    .collect(Collectors.joining("、"));
            return new ApiResult<>(WmsResultCode.STOCK_INSUFFICIENT.getCode(),
                    "以下SKU库存不足：" + skuList, shortages);
        }
        return ApiResult.ok();
    }

    @PatchMapping("/cancel")
    @PreAuthorize("@per.hasPermission('wms:sales-outbound:edit')")
    @OperationLog(bizType = "销售出库单管理", successMessage = "取消成功")
    @Operation(summary = "取消出库单")
    public ApiResult<Void> cancel(@RequestParam Long id) {
        coreModeGuard.assertLegacyWriteAllowed("旧销售出库取消");
        salesOutboundFacade.cancel(id);
        return ApiResult.ok();
    }

    @DeleteMapping
    @PreAuthorize("@per.hasPermission('wms:sales-outbound:del')")
    @OperationLog(bizType = "销售出库单管理", successMessage = "删除成功")
    @Operation(summary = "删除销售出库单")
    public ApiResult<Void> delete(@RequestBody List<Long> ids) {
        coreModeGuard.assertLegacyWriteAllowed("旧销售出库删除");
        salesOutboundFacade.delete(ids);
        return ApiResult.ok();
    }

    @GetMapping("/pending-orders")
    @PreAuthorize("@per.hasPermission('wms:sales-outbound:read')")
    @Operation(summary = "获取待出库订单列表（分页）")
    public ApiResult<PageResult<PendingOrderVO>> getPendingOrders(
            PageParam pageParam,
            @Validated PendingOrderQO qo) {

        // 1. 分页查询订单（skuCodes 过滤已在 XML 的 EXISTS 子查询中处理）
        PageResult<ErpOrder> orderPage = erpOrderService.getPendingOutboundOrdersPage(pageParam, qo);

        if (orderPage.getRecords().isEmpty()) {
            return ApiResult.ok(new PageResult<>(new ArrayList<>(), orderPage.getTotal()));
        }

        List<ErpOrder> orders = orderPage.getRecords();

        // 2. 批量加载 items（避免 N+1）
        List<Long> orderIds = orders.stream().map(ErpOrder::getId).collect(Collectors.toList());
        List<ErpOrderItem> allItems = orderItemMapper.selectByOrderIds(orderIds);
        Map<Long, List<ErpOrderItem>> itemMap = allItems.stream()
                .collect(Collectors.groupingBy(ErpOrderItem::getOrderId));

        // 3. 批量获取店铺名称
        List<Long> shopIds = orders.stream()
                .map(ErpOrder::getShopId).distinct().collect(Collectors.toList());
        Map<Long, String> shopNameMap = shopService.listByIds(shopIds).stream()
                .collect(Collectors.toMap(Shop::getId, Shop::getName, (a, b) -> a));

        // 4. SKU 编码：优先取订单明细自带的 sku_code；缺失时才回退按 platformItemId 查 sku_mapping 映射
        Set<String> platformItemIds = allItems.stream()
                .map(ErpOrderItem::getPlatformItemId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, String> skuCodeMap = platformItemIds.isEmpty()
                ? Collections.emptyMap()
                : skuMappingService.getSkuCodeMapByPlatformItemIds(platformItemIds);
        // 每个明细的最终 skuCode（明细自带优先），用于批量拉取 SKU 简要信息
        Set<String> skuCodes = allItems.stream()
                .map(item -> resolveSkuCode(item, skuCodeMap))
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, SkuBriefVO> skuBriefMap = skuBriefService.buildMapForQuery(skuCodes);

        // 5. 转换为 PendingOrderVO
        List<PendingOrderVO> voList = orders.stream().map(order -> {
            PendingOrderVO vo = new PendingOrderVO();
            vo.setId(order.getId());
            vo.setPlatformOrderId(order.getPlatformOrderId());
            vo.setPlatform(order.getPlatform());
            vo.setShopId(order.getShopId());
            vo.setShopName(shopNameMap.getOrDefault(order.getShopId(), ""));
            vo.setOrderTime(order.getPlatformCreatedAt() != null
                    ? order.getPlatformCreatedAt().toString() : null);

            // 构建 items
            List<ErpOrderItem> orderItems = itemMap.getOrDefault(order.getId(), Collections.emptyList());
            List<PendingOrderItemVO> itemVOs = orderItems.stream().map(item -> {
                PendingOrderItemVO itemVO = new PendingOrderItemVO();
                itemVO.setPlatformItemId(item.getPlatformItemId());
                String skuCode = resolveSkuCode(item, skuCodeMap);
                itemVO.setSkuCode(skuCode);
                itemVO.setQuantity(item.getQuantity());
                if (StringUtils.hasText(skuCode)) {
                    itemVO.setSkuBrief(skuBriefMap.get(skuCode));
                }
                return itemVO;
            }).collect(Collectors.toList());
            vo.setItems(itemVOs);
            vo.setTotalQuantity(itemVOs.stream().mapToInt(PendingOrderItemVO::getQuantity).sum());

            return vo;
        }).collect(Collectors.toList());

        // 6. 批量查询库存 + 填充库存状态
        enrichStockStatus(voList, qo.getWarehouseId());

        return ApiResult.ok(new PageResult<>(voList, orderPage.getTotal()));
    }

    /**
     * 解析订单明细的 SKU 编码：优先明细自带的 sku_code，缺失时回退 platformItemId → sku_mapping 映射。
     * 当前订单同步落库时 sku_code 已直接写入 erp_order_item，platform_item_id 多为空，故以明细自带为准。
     */
    private String resolveSkuCode(ErpOrderItem item, Map<String, String> skuCodeMap) {
        if (StringUtils.hasText(item.getSkuCode())) {
            return item.getSkuCode();
        }
        return skuCodeMap.get(item.getPlatformItemId());
    }

    /**
     * 填充库存状态（按 item 的 skuCode 查库存，汇总到订单级别）
     */
    private void enrichStockStatus(List<PendingOrderVO> voList, Long warehouseId) {
        // 收集所有 skuCode
        Set<String> allSkuCodes = voList.stream()
                .filter(vo -> vo.getItems() != null)
                .flatMap(vo -> vo.getItems().stream())
                .map(PendingOrderItemVO::getSkuCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        if (allSkuCodes.isEmpty()) return;

        // 待发货列表为货主自身视角：按当前货主取库存，防多货主共享自有仓时跨货主串数（H-7）
        Long erpTenantId = com.erp.admin.common.tenant.TenantContext.getCurrentTenant();
        Map<String, Integer> stockMap = physicalInventoryService.getAllocatableQuantityMap(
                erpTenantId, warehouseId, new ArrayList<>(allSkuCodes));

        for (PendingOrderVO vo : voList) {
            if (vo.getItems() == null || vo.getItems().isEmpty()) continue;

            int minAvailable = Integer.MAX_VALUE;
            boolean allSufficient = true;
            boolean allZero = true;

            for (PendingOrderItemVO item : vo.getItems()) {
                String skuCode = item.getSkuCode();
                int available = StringUtils.hasText(skuCode) ? stockMap.getOrDefault(skuCode, 0) : 0;
                int required = item.getQuantity() != null ? item.getQuantity() : 0;

                item.setAvailableStock(available);
                item.setStockStatus(StockStatus.calc(available, required).getValue());

                minAvailable = Math.min(minAvailable, available);
                if (available < required) allSufficient = false;
                if (available > 0) allZero = false;
            }

            // 订单级别库存状态：任一 item 不足则 insufficient，全部为零则 zero
            vo.setAvailableStock(minAvailable == Integer.MAX_VALUE ? 0 : minAvailable);
            if (allZero) {
                vo.setStockStatus(StockStatus.ZERO.getValue());
            } else if (allSufficient) {
                vo.setStockStatus(StockStatus.SUFFICIENT.getValue());
            } else {
                vo.setStockStatus(StockStatus.INSUFFICIENT.getValue());
            }
        }
    }

    @GetMapping("/export")
    @ResponseExcel(name = "销售出库单-#{currentDateTime()}")
    @PreAuthorize("@per.hasPermission('wms:sales-outbound:read')")
    @Operation(summary = "导出销售出库单")
    public List<SalesOutboundExportVO> export(SalesOutboundQO qo) {
        return salesOutboundService.listForExport(qo);
    }

}
