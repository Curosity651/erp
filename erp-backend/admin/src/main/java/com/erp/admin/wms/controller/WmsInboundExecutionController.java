package com.erp.admin.wms.controller;

import com.erp.admin.wms.model.dto.InboundPutawayDTO;
import com.erp.admin.wms.model.dto.InboundReceiveDTO;
import com.erp.admin.wms.model.qo.PurchaseInboundQO;
import com.erp.admin.wms.model.vo.AvailableLocationVO;
import com.erp.admin.wms.model.vo.PurchaseInboundDetailVO;
import com.erp.admin.wms.model.vo.PurchaseInboundPageVO;
import com.erp.admin.wms.service.PurchaseInboundService;
import com.erp.admin.wms.service.WmsInboundExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
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

	@Operation(summary = "上架可选库位(本货主服务商租用货架上、空闲且分区匹配品质, 库位独占)")
	@GetMapping("/available-locations")
	@PreAuthorize("hasAuthority('wms:inbound-exec:oper')")
	public ApiResult<List<AvailableLocationVO>> availableLocations(@RequestParam Long inboundOrderId,
			@RequestParam(required = false) String quality) {
		return ApiResult.ok(this.inboundExecutionService.listAvailableLocations(inboundOrderId, quality));
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
	public ApiResult<Void> putaway(@Validated @RequestBody InboundPutawayDTO dto) {
		inboundExecutionService.putaway(dto);
		return ApiResult.ok();
	}

}
