package com.erp.admin.wms.controller;

import com.erp.admin.wms.config.WmsCoreModeGuard;
import com.erp.admin.wms.model.dto.PackDTO;
import com.erp.admin.wms.model.dto.PackPackageDTO;
import com.erp.admin.wms.model.dto.PackageScanDTO;
import com.erp.admin.wms.model.dto.PackageLabelScanDTO;
import com.erp.admin.wms.model.dto.ShipPackageDTO;
import com.erp.admin.wms.model.dto.ShipDTO;
import com.erp.admin.wms.model.qo.PackShipQO;
import com.erp.admin.wms.model.qo.PackShipPackageQO;
import com.erp.admin.wms.model.vo.LogisticsChannelVO;
import com.erp.admin.wms.model.vo.PackShipOrderVO;
import com.erp.admin.wms.model.vo.PackShipPackagePageVO;
import com.erp.admin.wms.model.vo.ShipResultVO;
import com.erp.admin.order.model.vo.LabelBatchVO;
import com.erp.admin.order.model.vo.OzonActBatchVO;
import com.erp.admin.wms.service.OutboundShippingService;
import com.erp.admin.wms.service.WarehouseOutboundDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;

/**
 * 海外仓平台出库作业·打包签出控制器（业务需求 1.3.2）。仅平台身份（service 内二次校验）。
 *
 * @author erp
 */
@Tag(name = "出库作业·打包签出")
@RestController
@RequestMapping("/wms/outbound-shipping")
@RequiredArgsConstructor
public class OutboundShippingController {

    private final OutboundShippingService outboundShippingService;
	private final WarehouseOutboundDocumentService outboundDocumentService;
	private final PrincipalAttributeAccessor principalAttributeAccessor;
	private final WmsCoreModeGuard coreModeGuard;

    @Operation(summary = "分页 拣货中/已打包/已发货 订单")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<PageResult<PackShipOrderVO>> page(PageParam pageParam, PackShipQO qo) {
        return ApiResult.ok(outboundShippingService.page(pageParam, qo));
    }

	@Operation(summary = "按平台订单包裹分页查询打包签出任务")
	@GetMapping("/package-page")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<PageResult<PackShipPackagePageVO>> packagePage(
			PageParam pageParam, PackShipPackageQO qo) {
		return ApiResult.ok(outboundShippingService.pagePackages(pageParam, qo));
	}

	@Operation(summary = "扫描格口码或平台订单号定位包裹")
	@GetMapping("/package/locate")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<PackShipPackagePageVO> locatePackage(@RequestParam String scanCode) {
		return ApiResult.ok(outboundShippingService.locatePackage(scanCode));
	}

    @Operation(summary = "订单详情(含明细)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<PackShipOrderVO> detail(@PathVariable("id") Long id) {
        return ApiResult.ok(outboundShippingService.getDetail(id));
    }

    @Operation(summary = "打包 PICKING→PACKED")
    @PostMapping("/pack")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<Void> pack(@Validated @RequestBody PackDTO dto) {
        coreModeGuard.assertLegacyWriteAllowed("旧出库打包");
        outboundShippingService.pack(dto);
        return ApiResult.ok();
    }

	@Operation(summary = "销售出库按平台订单逐包裹打包")
	@PostMapping("/pack-package")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> packPackage(@Validated @RequestBody PackPackageDTO dto) {
		coreModeGuard.assertLegacyWriteAllowed("旧出库包裹打包");
		outboundShippingService.packPackage(dto, principalAttributeAccessor.getUserId());
		return ApiResult.ok();
	}

	@Operation(summary = "销售出库包裹扫码复核")
	@PostMapping("/pack-package/scan")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper') and (#dto.manual != true or hasAuthority('wms:outbound-exec:supervise'))")
	public ApiResult<Void> scanPackPackage(@Validated @RequestBody PackageScanDTO dto) {
		coreModeGuard.assertLegacyWriteAllowed("旧出库包裹复核");
		outboundShippingService.scanPackPackage(dto, principalAttributeAccessor.getUserId());
		return ApiResult.ok();
	}

	@Operation(summary = "扫描平台面单并确认贴附到正确包裹")
	@PostMapping("/pack-package/label-scan")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> confirmPackageLabel(@Validated @RequestBody PackageLabelScanDTO dto) {
		coreModeGuard.assertLegacyWriteAllowed("旧出库面单确认");
		outboundShippingService.confirmPackageLabel(dto, principalAttributeAccessor.getUserId());
		return ApiResult.ok();
	}

	@Operation(summary = "核对货主外部提供的平台面单")
	@PostMapping("/external-document")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> confirmExternalDocument(@RequestParam Long outboundOrderId,
			@RequestParam Long packageId) {
		coreModeGuard.assertLegacyWriteAllowed("旧出库外部面单确认");
		outboundShippingService.confirmExternalDocument(outboundOrderId, packageId);
		return ApiResult.ok();
	}

	@Operation(summary = "核对货主外部提供的平台交接单")
	@PostMapping("/external-handover")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> confirmExternalHandover(@RequestParam Long outboundOrderId,
			@RequestParam Long packageId) {
		coreModeGuard.assertLegacyWriteAllowed("旧出库交接单确认");
		outboundShippingService.confirmExternalHandover(outboundOrderId, packageId);
		return ApiResult.ok();
	}

	@PostMapping("/documents/labels")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	@Operation(summary = "按销售出库单生成平台面单")
	public ApiResult<LabelBatchVO> prepareLabels(@RequestParam Long outboundOrderId) {
		coreModeGuard.assertLegacyWriteAllowed("旧出库面单生成");
		return ApiResult.ok(outboundDocumentService.prepareLabels(
				outboundOrderId, principalAttributeAccessor.getUserId()));
	}

	@GetMapping("/documents/labels/latest")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	@Operation(summary = "查询销售出库单最近一次平台面单生成结果")
	public ApiResult<LabelBatchVO> latestLabels(@RequestParam Long outboundOrderId) {
		return ApiResult.ok(outboundDocumentService.latestLabels(outboundOrderId));
	}

	@PostMapping("/documents/ozon-act")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	@Operation(summary = "生成销售出库单所需的 Ozon 交接单")
	public ApiResult<OzonActBatchVO> prepareOzonAct(@RequestParam Long outboundOrderId,
			@RequestParam LocalDate departureDate) {
		coreModeGuard.assertLegacyWriteAllowed("旧出库Ozon交接单生成");
		return ApiResult.ok(outboundDocumentService.prepareOzonActs(
				outboundOrderId, departureDate, principalAttributeAccessor.getUserId()));
	}

	@GetMapping("/documents/ozon-act")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	@Operation(summary = "轮询销售出库单 Ozon 交接单")
	public ApiResult<OzonActBatchVO> pollOzonAct(@RequestParam Long outboundOrderId,
			@RequestParam String batchNo) {
		return ApiResult.ok(outboundDocumentService.pollOzonActs(outboundOrderId, batchNo));
	}

    @Operation(summary = "签出 PACKED→SHIPPED(扣库存+释放锁定+计费)")
    @PostMapping("/ship")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<ShipResultVO> ship(@Validated @RequestBody ShipDTO dto) {
        coreModeGuard.assertLegacyWriteAllowed("旧出库签出");
        return ApiResult.ok(outboundShippingService.ship(dto, principalAttributeAccessor.getUserId()));
    }

	@Operation(summary = "按平台订单包裹独立签出")
	@PostMapping("/ship-package")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<ShipResultVO> shipPackage(@Validated @RequestBody ShipPackageDTO dto) {
		coreModeGuard.assertLegacyWriteAllowed("旧出库包裹签出");
		return ApiResult.ok(outboundShippingService.shipPackage(dto, principalAttributeAccessor.getUserId()));
	}

    @Operation(summary = "物流渠道选项")
    @GetMapping("/channels")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<List<LogisticsChannelVO>> channels() {
        return ApiResult.ok(outboundShippingService.listChannels());
    }

}
