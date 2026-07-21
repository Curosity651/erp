package com.erp.admin.wms.controller;

import com.erp.admin.wms.facade.PurchaseInboundFacade;
import com.erp.admin.wms.model.dto.CustomReturnDTO;
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
 * 自定义退货单管理
 * <p>
 * 与采购入库单共用 {@code wms_purchase_inbound_order} 表（{@code source_type=CUSTOM_RETURN}），用于不挂靠
 * 平台订单的退货入库（平台批量退货混包、无单退件、样品收回、发错召回等）。建单→提交后同样流转给平台收货/上架。
 * </p>
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/custom-return")
@Tag(name = "自定义退货单管理")
public class CustomReturnController {

    private final PurchaseInboundService purchaseInboundService;

    private final PurchaseInboundFacade purchaseInboundFacade;

    /**
     * 分页查询
     */
    @Operation(summary = "分页查询")
    @GetMapping("/page")
    @PreAuthorize("@per.hasPermission('wms:custom-return:read')")
    public ApiResult<PageResult<PurchaseInboundPageVO>> getPage(PageParam pageParam, PurchaseInboundQO qo) {
        qo.setSourceType(InboundSourceType.CUSTOM_RETURN.name());
        return ApiResult.ok(this.purchaseInboundService.queryPage(pageParam, qo));
    }

    /**
     * 退货单详情
     */
    @Operation(summary = "退货单详情")
    @GetMapping("/detail")
    @PreAuthorize("@per.hasPermission('wms:custom-return:read')")
    public ApiResult<PurchaseInboundDetailVO> getDetail(@RequestParam Long id) {
        return ApiResult.ok(this.purchaseInboundService.getDetail(id));
    }

    /**
     * 新建自定义退货单
     */
    @Operation(summary = "新建自定义退货单")
    @OperationLog(bizType = "自定义退货单管理", successMessage = "新建成功")
    @PostMapping
    @PreAuthorize("@per.hasPermission('wms:custom-return:add')")
    public ApiResult<Long> create(@Validated({Default.class, CreateGroup.class}) @RequestBody CustomReturnDTO dto) {
        return ApiResult.ok(this.purchaseInboundFacade.createCustomReturn(dto));
    }

    /**
     * 编辑自定义退货单
     */
    @Operation(summary = "编辑自定义退货单")
    @OperationLog(bizType = "自定义退货单管理", successMessage = "编辑成功")
    @PutMapping
    @PreAuthorize("@per.hasPermission('wms:custom-return:edit')")
    public ApiResult<Void> update(@Validated({Default.class, UpdateGroup.class}) @RequestBody CustomReturnDTO dto) {
        this.purchaseInboundService.updateCustomReturnOrder(dto);
        return ApiResult.ok();
    }

    /**
     * 提交退货单（草稿 → 已提交）
     */
    @Operation(summary = "提交退货单")
    @OperationLog(bizType = "自定义退货单管理", successMessage = "提交成功")
    @PatchMapping("/submit")
    @PreAuthorize("@per.hasPermission('wms:custom-return:edit')")
    public ApiResult<Void> submit(@RequestParam Long id) {
        this.purchaseInboundFacade.submit(id);
        return ApiResult.ok();
    }

    /**
     * 取消退货单
     */
    @Operation(summary = "取消退货单")
    @OperationLog(bizType = "自定义退货单管理", successMessage = "取消成功")
    @PatchMapping("/cancel")
    @PreAuthorize("@per.hasPermission('wms:custom-return:edit')")
    public ApiResult<Void> cancel(@RequestParam Long id) {
        this.purchaseInboundFacade.cancel(id);
        return ApiResult.ok();
    }

    /**
     * 删除退货单
     */
    @Operation(summary = "删除退货单")
    @OperationLog(bizType = "自定义退货单管理", successMessage = "删除成功")
    @DeleteMapping
    @PreAuthorize("@per.hasPermission('wms:custom-return:del')")
    public ApiResult<Void> delete(@RequestBody List<Long> ids) {
        this.purchaseInboundFacade.delete(ids);
        return ApiResult.ok();
    }

    /**
     * 导出自定义退货单
     */
    @Operation(summary = "导出自定义退货单")
    @GetMapping("/export")
    @AccessLoggingRule(includeQueryString = true, includeRequestBody = true)
    @ResponseExcel(name = "自定义退货单-#{currentDateTime()}")
    @PreAuthorize("@per.hasPermission('wms:custom-return:read')")
    public List<PurchaseInboundPageVO> export(PurchaseInboundQO qo) {
        qo.setSourceType(InboundSourceType.CUSTOM_RETURN.name());
        return this.purchaseInboundService.listForExport(qo);
    }

}
