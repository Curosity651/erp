package com.erp.admin.wms.controller;

import com.erp.admin.wms.model.dto.InboundPutawayDTO;
import com.erp.admin.wms.model.dto.InboundReceiveDTO;
import com.erp.admin.wms.model.dto.PutawayRecordDTO;
import com.erp.admin.wms.model.entity.PurchaseInboundOrder;
import com.erp.admin.wms.model.qo.PurchaseInboundQO;
import com.erp.admin.wms.model.vo.AvailableLocationVO;
import com.erp.admin.wms.model.vo.PurchaseInboundDetailVO;
import com.erp.admin.wms.model.vo.PutawayReceiptLineVO;
import com.erp.admin.wms.model.vo.PurchaseInboundPageVO;
import com.erp.admin.wms.model.vo.InboundPutawayPlanVO;
import com.erp.admin.wms.model.vo.LogicalInboundPutawayPlanVO;
import com.erp.admin.wms.model.vo.PutawayRecordContextVO;
import com.erp.admin.wms.model.vo.PalletSummaryVO;
import com.erp.admin.wms.service.PurchaseInboundService;
import com.erp.admin.wms.service.WmsInboundExecutionService;
import com.erp.admin.wms.service.LogicalInboundPutawayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.common.core.exception.BusinessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台入库作业控制器（D2.4，收货 / 上架）。
 *
 * <p>仅平台超管可作业（服务层 {@code assertPlatform} 二次校验）。
 *
 * @author erp
 */
@Tag(name = "平台入库作业(收货/上架)")
@RestController
@RequestMapping("/wms/inbound-execution")
@RequiredArgsConstructor
public class WmsInboundExecutionController {

	private final WmsInboundExecutionService inboundExecutionService;

	private final LogicalInboundPutawayService logicalPutawayService;

	private final PurchaseInboundService purchaseInboundService;

	@Operation(summary = "平台待作业入库单分页(采购+自定义全来源, 平台看全部)")
	@GetMapping("/page")
	@PreAuthorize("hasAuthority('wms:inbound-exec:oper')")
	public ApiResult<PageResult<PurchaseInboundPageVO>> getPage(PageParam pageParam, PurchaseInboundQO qo) {
		// 不限定 source_type → 采购入库 + 自定义入库都纳入平台收货/上架视图；
		// 货主作用域由 service 强制为「平台看全部」。
		return ApiResult.ok(this.purchaseInboundService.queryPage(pageParam, qo));
	}

	@Operation(summary = "入库单详情(平台作业收货/上架用，平台看全部)")
	@GetMapping("/detail")
	@PreAuthorize("hasAuthority('wms:inbound-exec:oper')")
	public ApiResult<PurchaseInboundDetailVO> getDetail(@RequestParam Long id) {
		// 平台作业只需明细(应到/实到数量)；货主作用域由 service 强制为平台看全部
		return ApiResult.ok(this.purchaseInboundService.getDetail(id));
	}

	@Operation(summary = "Find a submitted inbound order by its printed number")
	@GetMapping("/scan")
	@PreAuthorize("hasAuthority('wms:inbound-exec:oper')")
	public ApiResult<PurchaseInboundDetailVO> scan(@RequestParam String inboundNo) {
		PurchaseInboundOrder order = inboundExecutionService.findSubmittedByInboundNo(inboundNo);
		return ApiResult.ok(purchaseInboundService.getDetail(order.getId()));
	}

	@Operation(summary = "上架可选库位(本货主服务商租用货架上、空闲且分区匹配品质, 库位独占)")
	@GetMapping("/available-locations")
	@PreAuthorize("hasAuthority('wms:inbound-exec:oper')")
	public ApiResult<List<AvailableLocationVO>> availableLocations(@RequestParam Long inboundOrderId,
			@RequestParam(required = false) String quality) {
		return ApiResult.ok(this.inboundExecutionService.listAvailableLocations(inboundOrderId, quality));
	}

	@Operation(summary = "Generate pallet split and three-level slot recommendation")
	@GetMapping("/putaway-plan")
	@PreAuthorize("hasAuthority('wms:inbound-exec:oper')")
	public ApiResult<InboundPutawayPlanVO> putawayPlan(@RequestParam Long inboundOrderId) {
		throw legacyPutawayDisabled();
	}

	@GetMapping("/logical-putaway-plan")
	@PreAuthorize("hasAuthority('wms:inbound-exec:oper')")
	public ApiResult<LogicalInboundPutawayPlanVO> logicalPutawayPlan(@RequestParam Long inboundOrderId) {
		throw legacyPutawayDisabled();
	}

	@Operation(summary = "取得人工上架补录上下文")
	@GetMapping("/putaway-record-context")
	@PreAuthorize("hasAuthority('wms:inbound-exec:oper')")
	public ApiResult<PutawayRecordContextVO> putawayRecordContext(@RequestParam Long inboundOrderId) {
		return ApiResult.ok(logicalPutawayService.context(inboundOrderId));
	}

	@Operation(summary = "收货(录实收数量)")
	@PostMapping("/receive")
	@PreAuthorize("hasAuthority('wms:inbound-exec:oper')")
	public ApiResult<Void> receive(@Validated @RequestBody InboundReceiveDTO dto) {
		inboundExecutionService.receive(dto);
		return ApiResult.ok();
	}

	@Operation(summary = "上架(分配库位写批次)")
	@PostMapping("/putaway")
	@PreAuthorize("hasAuthority('wms:inbound-exec:oper')")
	public ApiResult<List<PalletSummaryVO>> putaway(@Validated @RequestBody InboundPutawayDTO dto) {
		throw legacyPutawayDisabled();
	}

	@PostMapping("/logical-putaway")
	@PreAuthorize("hasAuthority('wms:inbound-exec:oper')")
	public ApiResult<List<PutawayReceiptLineVO>> logicalPutaway(@Validated @RequestBody InboundPutawayDTO dto) {
		throw legacyPutawayDisabled();
	}

	@Operation(summary = "登记现场实际上架结果")
	@PostMapping("/putaway-record")
	@PreAuthorize("hasAuthority('wms:inbound-exec:oper')")
	public ApiResult<List<PutawayReceiptLineVO>> putawayRecord(@Validated @RequestBody PutawayRecordDTO dto) {
		return ApiResult.ok(logicalPutawayService.record(dto));
	}

	@Operation(summary = "查询已上架入库单关联托盘")
	@GetMapping("/putaway-pallets")
	@PreAuthorize("hasAuthority('wms:inbound-exec:oper')")
	public ApiResult<List<PalletSummaryVO>> getPutawayPallets(@RequestParam Long inboundOrderId) {
		return ApiResult.ok(inboundExecutionService.getPutawayPallets(inboundOrderId));
	}

	@Operation(summary = "查询已上架入库单的本次上架明细")
	@GetMapping("/putaway-receipt-lines")
	@PreAuthorize("hasAuthority('wms:inbound-exec:oper')")
	public ApiResult<List<PutawayReceiptLineVO>> getPutawayReceiptLines(@RequestParam Long inboundOrderId) {
		return ApiResult.ok(inboundExecutionService.getPutawayReceiptLines(inboundOrderId));
	}

	private BusinessException legacyPutawayDisabled() {
		return new BusinessException(410, "旧版上架模式已停用，请使用上架记录功能");
	}

}
