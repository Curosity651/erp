package com.erp.admin.wms.controller;

import com.erp.admin.wms.facade.PurchaseInboundFacade;
import com.erp.admin.wms.model.dto.ManualInboundDTO;
import com.erp.admin.wms.model.enums.InboundSourceType;
import com.erp.admin.wms.model.qo.PurchaseInboundQO;
import com.erp.admin.wms.model.vo.PurchaseInboundDetailVO;
import com.erp.admin.wms.model.vo.PurchaseInboundPageVO;
import com.erp.admin.wms.service.PurchaseInboundService;
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
import java.util.List;

/**
 * 自定义入库单管理
 * <p>
 * 与采购入库单共用 {@code wms_purchase_inbound_order} 表（{@code source_type=MANUAL}），用于不经过
 * 「采购 → 物流 → 入库」完整链路的入库。建单→提交后同样流转给平台收货/上架。
 * </p>
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/manual-inbound")
@Tag(name = "自定义入库单管理")
public class ManualInboundController {

    private final PurchaseInboundService purchaseInboundService;

    private final PurchaseInboundFacade purchaseInboundFacade;

    /**
     * 分页查询
     */
    @Operation(summary = "分页查询")
    @GetMapping("/page")
    @PreAuthorize("@per.hasPermission('wms:manual-inbound:read')")
    public ApiResult<PageResult<PurchaseInboundPageVO>> getPage(PageParam pageParam, PurchaseInboundQO qo) {
        qo.setSourceType(InboundSourceType.MANUAL.name());
        return ApiResult.ok(this.purchaseInboundService.queryPage(pageParam, qo));
    }

    /**
     * 入库单详情
     */
    @Operation(summary = "入库单详情")
    @GetMapping("/detail")
    @PreAuthorize("@per.hasPermission('wms:manual-inbound:read')")
    public ApiResult<PurchaseInboundDetailVO> getDetail(@RequestParam Long id) {
        return ApiResult.ok(this.purchaseInboundService.getDetail(id, InboundSourceType.MANUAL));
    }

    /**
     * 新建自定义入库单
     */
    @Operation(summary = "新建自定义入库单")
    @OperationLog(bizType = "自定义入库单管理", successMessage = "新建成功")
    @PostMapping
    @PreAuthorize("@per.hasPermission('wms:manual-inbound:add')")
    public ApiResult<Long> create(@Validated({Default.class, CreateGroup.class}) @RequestBody ManualInboundDTO dto) {
        return ApiResult.ok(this.purchaseInboundFacade.createManual(dto));
    }

    /**
     * 编辑自定义入库单
     */
    @Operation(summary = "编辑自定义入库单")
    @OperationLog(bizType = "自定义入库单管理", successMessage = "编辑成功")
    @PutMapping
    @PreAuthorize("@per.hasPermission('wms:manual-inbound:edit')")
    public ApiResult<Void> update(@Validated({Default.class, UpdateGroup.class}) @RequestBody ManualInboundDTO dto) {
        this.purchaseInboundFacade.updateManual(dto);
        return ApiResult.ok();
    }

    /**
     * 提交入库单（草稿 → 已提交）
     */
    @Operation(summary = "提交入库单")
    @OperationLog(bizType = "自定义入库单管理", successMessage = "提交成功")
    @PatchMapping("/submit")
    @PreAuthorize("@per.hasPermission('wms:manual-inbound:edit')")
    public ApiResult<Void> submit(@RequestParam Long id) {
        this.purchaseInboundFacade.submit(id, InboundSourceType.MANUAL);
        return ApiResult.ok();
    }

    /**
     * 取消入库单
     */
    @Operation(summary = "取消入库单")
    @OperationLog(bizType = "自定义入库单管理", successMessage = "取消成功")
    @PatchMapping("/cancel")
    @PreAuthorize("@per.hasPermission('wms:manual-inbound:edit')")
    public ApiResult<Void> cancel(@RequestParam Long id) {
        this.purchaseInboundFacade.cancel(id, InboundSourceType.MANUAL);
        return ApiResult.ok();
    }

    /**
     * 删除入库单
     */
    @Operation(summary = "删除入库单")
    @OperationLog(bizType = "自定义入库单管理", successMessage = "删除成功")
    @DeleteMapping
    @PreAuthorize("@per.hasPermission('wms:manual-inbound:del')")
    public ApiResult<Void> delete(@RequestBody List<Long> ids) {
        this.purchaseInboundFacade.delete(ids, InboundSourceType.MANUAL);
        return ApiResult.ok();
    }

    /**
     * 导出自定义入库单
     */
    @Operation(summary = "导出自定义入库单")
    @GetMapping("/export")
    @AccessLoggingRule(includeQueryString = true, includeRequestBody = true)
    @ResponseExcel(name = "自定义入库单-#{currentDateTime()}")
    @PreAuthorize("@per.hasPermission('wms:manual-inbound:read')")
    public List<PurchaseInboundPageVO> export(PurchaseInboundQO qo) {
        qo.setSourceType(InboundSourceType.MANUAL.name());
        return this.purchaseInboundService.listForExport(qo);
    }

}
