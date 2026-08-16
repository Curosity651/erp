package com.erp.admin.wms.controller;

import com.erp.admin.wms.enums.WmsResultCode;
import com.erp.admin.wms.config.WmsCoreModeGuard;
import com.erp.admin.wms.facade.CustomOutboundFacade;
import com.erp.admin.wms.model.dto.BatchStockQueryDTO;
import com.erp.admin.wms.model.dto.CustomOutboundDTO;
import com.erp.admin.wms.model.qo.CustomOutboundQO;
import com.erp.admin.wms.model.vo.CustomOutboundDetailVO;
import com.erp.admin.wms.model.vo.CustomOutboundPageVO;
import com.erp.admin.wms.model.vo.StockShortageVO;
import com.erp.admin.wms.service.CustomOutboundService;
import com.erp.admin.wms.service.ErpOwnerScopeService;
import com.erp.admin.wms.service.WmsPhysicalInventoryService;
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
import org.ballcat.web.accesslog.annotation.AccessLoggingRule;
import org.springframework.security.access.prepost.PreAuthorize;
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

import javax.validation.groups.Default;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 自定义出库单管理
 * <p>
 * 与销售出库单共用 {@code wms_sales_outbound_order} 表（{@code source_type=CUSTOM}），
 * 用于不挂平台订单的出库（线下/独立站订单发货、样品寄送、销毁报废、退供应商等）。
 * 提交（校验可售 + 预占）后与销售出库单同链路流转至海外仓平台作业台（下架→打包→签出）。
 * </p>
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/custom-outbound")
@Tag(name = "自定义出库单管理")
public class CustomOutboundController {

    private final CustomOutboundService customOutboundService;

    private final CustomOutboundFacade customOutboundFacade;

    private final WmsPhysicalInventoryService physicalInventoryService;

    private final ErpOwnerScopeService erpOwnerScopeService;

    private final WmsCoreModeGuard coreModeGuard;

    /**
     * 分页查询（数据权限已按货主隔离，查询强制 source_type=CUSTOM）
     */
    @Operation(summary = "分页查询自定义出库单")
    @GetMapping("/page")
    @PreAuthorize("@per.hasPermission('wms:custom-outbound:read')")
    public ApiResult<PageResult<CustomOutboundPageVO>> getPage(PageParam pageParam, CustomOutboundQO qo) {
        return ApiResult.ok(customOutboundService.queryPage(pageParam, qo));
    }

    /**
     * 出库单详情
     */
    @Operation(summary = "获取自定义出库单详情")
    @GetMapping("/detail")
    @PreAuthorize("@per.hasPermission('wms:custom-outbound:read')")
    public ApiResult<CustomOutboundDetailVO> getDetail(@RequestParam Long id) {
        return ApiResult.ok(customOutboundService.getDetail(id));
    }

    /**
     * 新建自定义出库单（草稿）
     */
    @Operation(summary = "新建自定义出库单")
    @OperationLog(bizType = "自定义出库单管理", successMessage = "新建成功")
    @PostMapping
    @PreAuthorize("@per.hasPermission('wms:custom-outbound:add')")
    public ApiResult<Long> create(@Validated({Default.class, CreateGroup.class}) @RequestBody CustomOutboundDTO dto) {
        coreModeGuard.assertLegacyWriteAllowed("旧自定义出库创建");
        return ApiResult.ok(customOutboundFacade.create(dto));
    }

    /**
     * 编辑自定义出库单（仅草稿）
     */
    @Operation(summary = "编辑自定义出库单")
    @OperationLog(bizType = "自定义出库单管理", successMessage = "编辑成功")
    @PutMapping
    @PreAuthorize("@per.hasPermission('wms:custom-outbound:edit')")
    public ApiResult<Void> update(@Validated({Default.class, UpdateGroup.class}) @RequestBody CustomOutboundDTO dto) {
        coreModeGuard.assertLegacyWriteAllowed("旧自定义出库编辑");
        customOutboundFacade.update(dto);
        return ApiResult.ok();
    }

    /**
     * 提交出库单（校验可售库存 + 预占，DRAFT→CONFIRMED，流转至平台作业台）
     */
    @Operation(summary = "提交自定义出库单")
    @OperationLog(bizType = "自定义出库单管理", successMessage = "提交成功")
    @PatchMapping("/submit")
    @PreAuthorize("@per.hasPermission('wms:custom-outbound:edit')")
    public ApiResult<List<StockShortageVO>> submit(@RequestParam Long id) {
        coreModeGuard.assertLegacyWriteAllowed("旧自定义出库提交");
        List<StockShortageVO> shortages = customOutboundFacade.submit(id);
        if (!shortages.isEmpty()) {
            // 构建错误消息
            String skuList = shortages.stream()
                    .map(s -> s.getSkuName() + "(缺" + s.getShortage() + ")")
                    .collect(Collectors.joining("、"));
            return new ApiResult<>(WmsResultCode.STOCK_INSUFFICIENT.getCode(),
                    "以下SKU可售库存不足：" + skuList, shortages);
        }
        return ApiResult.ok();
    }

    /**
     * 取消出库单（草稿直接取消；已提交未下架的释放预占后取消）
     */
    @Operation(summary = "取消自定义出库单")
    @OperationLog(bizType = "自定义出库单管理", successMessage = "取消成功")
    @PatchMapping("/cancel")
    @PreAuthorize("@per.hasPermission('wms:custom-outbound:edit')")
    public ApiResult<Void> cancel(@RequestParam Long id) {
        coreModeGuard.assertLegacyWriteAllowed("旧自定义出库取消");
        customOutboundFacade.cancel(id);
        return ApiResult.ok();
    }

    /**
     * 删除出库单（仅草稿）
     */
    @Operation(summary = "删除自定义出库单")
    @OperationLog(bizType = "自定义出库单管理", successMessage = "删除成功")
    @DeleteMapping
    @PreAuthorize("@per.hasPermission('wms:custom-outbound:del')")
    public ApiResult<Void> delete(@RequestBody List<Long> ids) {
        coreModeGuard.assertLegacyWriteAllowed("旧自定义出库删除");
        customOutboundFacade.delete(ids);
        return ApiResult.ok();
    }

    /**
     * 导出自定义出库单
     */
    @Operation(summary = "导出自定义出库单")
    @GetMapping("/export")
    @AccessLoggingRule(includeQueryString = true, includeRequestBody = true)
    @ResponseExcel(name = "自定义出库单-#{currentDateTime()}")
    @PreAuthorize("@per.hasPermission('wms:custom-outbound:read')")
    public List<CustomOutboundPageVO> export(CustomOutboundQO qo) {
        return customOutboundService.listForExport(qo);
    }

    /**
     * 批量查询可售库存（表单页明细行实时展示当前货主在所选仓库的可售余量）
     */
    @Operation(summary = "批量查询可售库存")
    @PostMapping("/available-stock/batch")
    @PreAuthorize("@per.hasPermission('wms:custom-outbound:read')")
    public ApiResult<Map<String, Integer>> batchQueryStock(@Validated @RequestBody BatchStockQueryDTO dto) {
        // 按当前货主隔离取数，避免同仓多货主同名 SKU 折叠
        Long erpTenantId = erpOwnerScopeService.writeOwner();
        Map<String, Integer> stockMap = physicalInventoryService
                .getAllocatableQuantityMap(erpTenantId, dto.getWarehouseId(), dto.getSkuCodes());
        Map<String, Integer> result = new HashMap<>();
        for (String skuCode : dto.getSkuCodes()) {
            result.put(skuCode, stockMap.getOrDefault(skuCode, 0));
        }
        return ApiResult.ok(result);
    }

}
