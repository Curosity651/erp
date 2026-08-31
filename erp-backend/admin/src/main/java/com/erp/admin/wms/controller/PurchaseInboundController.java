package com.erp.admin.wms.controller;

import com.erp.admin.wms.facade.PurchaseInboundFacade;
import com.erp.admin.wms.model.dto.PurchaseInboundDTO;
import com.erp.admin.wms.model.enums.InboundSourceType;
import com.erp.admin.wms.model.qo.AvailableShippingQO;
import com.erp.admin.wms.model.qo.PurchaseInboundQO;
import com.erp.admin.wms.model.vo.AvailableShippingVO;
import com.erp.admin.wms.model.vo.PurchaseInboundDetailVO;
import com.erp.admin.wms.model.vo.PurchaseInboundPageVO;
import com.erp.admin.wms.model.vo.ShippingItemForInboundVO;
import com.erp.admin.wms.service.PurchaseInboundService;
import com.erp.admin.wms.service.ShippingOrderService;
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
 * 采购入库单管理
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/purchase-inbound")
@Tag(name = "采购入库单管理")
public class PurchaseInboundController {

    private final PurchaseInboundService purchaseInboundService;

    private final PurchaseInboundFacade purchaseInboundFacade;

	private final ShippingOrderService shippingOrderService;

    /**
     * 分页查询
     * @param pageParam 分页参数
     * @param qo 入库单查询对象
     * @return ApiResult 通用返回体
     */
    @Operation(summary = "分页查询")
    @GetMapping("/page")
    @PreAuthorize("@per.hasPermission('wms:purchase-inbound:read')")
    public ApiResult<PageResult<PurchaseInboundPageVO>> getPage(PageParam pageParam, PurchaseInboundQO qo) {
        qo.setSourceType(InboundSourceType.PURCHASE.name());
        return ApiResult.ok(this.purchaseInboundService.queryPage(pageParam, qo));
    }

    /**
     * 入库单详情
     * @param id 入库单ID
     * @return ApiResult 通用返回体
     */
    @Operation(summary = "入库单详情")
    @GetMapping("/detail")
    @PreAuthorize("@per.hasPermission('wms:purchase-inbound:read')")
    public ApiResult<PurchaseInboundDetailVO> getDetail(@RequestParam Long id) {
        PurchaseInboundDetailVO detail = this.purchaseInboundService.getDetail(id, InboundSourceType.PURCHASE);

        // Controller 层组装跨服务数据：填充物流单扩展信息
        if (detail.getShippingOrderId() != null) {
            AvailableShippingVO shippingOrder = this.shippingOrderService.getShippingById(detail.getShippingOrderId());
            detail.setShippingOrder(shippingOrder);
        }

        return ApiResult.ok(detail);
    }

    /**
     * 新建入库单
     * @param dto 入库单数据传输对象
     * @return ApiResult 通用返回体
     */
    @Operation(summary = "新建入库单")
    @OperationLog(bizType = "采购入库单管理", successMessage = "新建成功")
    @PostMapping
    @PreAuthorize("@per.hasPermission('wms:purchase-inbound:add')")
    public ApiResult<Long> create(@Validated({Default.class, CreateGroup.class}) @RequestBody PurchaseInboundDTO dto) {
        Long id = this.purchaseInboundFacade.create(dto);
        return ApiResult.ok(id);
    }

    /**
     * 编辑入库单
     * @param dto 入库单数据传输对象
     * @return ApiResult 通用返回体
     */
    @Operation(summary = "编辑入库单")
    @OperationLog(bizType = "采购入库单管理", successMessage = "编辑成功")
    @PutMapping
    @PreAuthorize("@per.hasPermission('wms:purchase-inbound:edit')")
    public ApiResult<Void> update(@Validated({Default.class, UpdateGroup.class}) @RequestBody PurchaseInboundDTO dto) {
        this.purchaseInboundFacade.updatePurchase(dto);
        return ApiResult.ok();
    }

    /**
     * 提交入库单（草稿 → 已提交），提交后流转给平台收货/上架
     * @param id 入库单ID
     * @return ApiResult 通用返回体
     */
    @Operation(summary = "提交入库单")
    @OperationLog(bizType = "采购入库单管理", successMessage = "提交成功")
    @PatchMapping("/submit")
    @PreAuthorize("@per.hasPermission('wms:purchase-inbound:edit')")
    public ApiResult<Void> submit(@RequestParam Long id) {
        this.purchaseInboundFacade.submit(id, InboundSourceType.PURCHASE);
        return ApiResult.ok();
    }

    /**
     * 取消入库单
     * @param id 入库单ID
     * @return ApiResult 通用返回体
     */
    @Operation(summary = "取消入库单")
    @OperationLog(bizType = "采购入库单管理", successMessage = "取消成功")
    @PatchMapping("/cancel")
    @PreAuthorize("@per.hasPermission('wms:purchase-inbound:edit')")
    public ApiResult<Void> cancel(@RequestParam Long id) {
        this.purchaseInboundFacade.cancel(id, InboundSourceType.PURCHASE);
        return ApiResult.ok();
    }

    /**
     * 删除入库单
     * @param ids 入库单ID列表
     * @return ApiResult 通用返回体
     */
    @Operation(summary = "删除入库单")
    @OperationLog(bizType = "采购入库单管理", successMessage = "删除成功")
    @DeleteMapping
    @PreAuthorize("@per.hasPermission('wms:purchase-inbound:del')")
    public ApiResult<Void> delete(@RequestBody List<Long> ids) {
        this.purchaseInboundFacade.delete(ids, InboundSourceType.PURCHASE);
        return ApiResult.ok();
    }

    /**
     * 获取物流单待入库明细
     * @param shippingOrderId 物流单ID
     * @return ApiResult 通用返回体
     */
    @Operation(summary = "获取物流单待入库明细")
    @GetMapping("/shipping-items")
    @PreAuthorize("@per.hasPermission('wms:purchase-inbound:read')")
    public ApiResult<List<ShippingItemForInboundVO>> getShippingItems(@RequestParam Long shippingOrderId) {
        return ApiResult.ok(this.purchaseInboundService.getShippingItems(shippingOrderId));
    }

    /**
     * 导出入库单
     * @param qo 查询条件
     * @return 导出数据列表
     */
    @Operation(summary = "导出入库单")
    @GetMapping("/export")
    @AccessLoggingRule(includeQueryString = true, includeRequestBody = true)
    @ResponseExcel(name = "采购入库单-#{currentDateTime()}")
    @PreAuthorize("@per.hasPermission('wms:purchase-inbound:read')")
    public List<PurchaseInboundPageVO> export(PurchaseInboundQO qo) {
        qo.setSourceType(InboundSourceType.PURCHASE.name());
        return this.purchaseInboundService.listForExport(qo);
    }


	/**
	 * 获取指定物流单信息（用于入库单创建页回填）
	 * @param shippingOrderId 物流单ID
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取指定物流单信息")
	@GetMapping("/shipping-detail")
	@PreAuthorize("@per.hasPermission('wms:purchase-inbound:read')")
	public ApiResult<AvailableShippingVO> getShippingDetail(@RequestParam Long shippingOrderId) {
		return ApiResult.ok(this.shippingOrderService.getShippingById(shippingOrderId));
	}



	/**
	 * 获取可入库物流单列表
	 * @param qo 查询条件
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取可入库物流单列表")
	@GetMapping("/available-shipping")
	@PreAuthorize("@per.hasPermission('wms:purchase-inbound:read')")
	public ApiResult<List<AvailableShippingVO>> getAvailableShipping(AvailableShippingQO qo) {
		return ApiResult.ok(this.shippingOrderService.getAvailableShipping(qo));
	}


}
