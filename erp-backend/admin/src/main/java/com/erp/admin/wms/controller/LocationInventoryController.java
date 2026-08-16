package com.erp.admin.wms.controller;

import java.util.List;
import java.util.Map;

import com.erp.admin.wms.model.vo.LocationInventoryDetailVO;
import com.erp.admin.wms.model.vo.LocationInventoryGridVO;
import com.erp.admin.wms.service.LocationInventoryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/location-inventory")
@Tag(name = "库位库存")
public class LocationInventoryController {

	private final LocationInventoryQueryService queryService;

	@GetMapping("/grid")
	@Operation(summary = "库位库存网格")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<List<LocationInventoryGridVO>> grid(@RequestParam("warehouseId") Long warehouseId) {
		return ApiResult.ok(queryService.grid(warehouseId));
	}

	@GetMapping("/tree")
	@Operation(summary = "按排分组的库位库存")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Map<String, List<LocationInventoryGridVO>>> tree(@RequestParam("warehouseId") Long warehouseId) {
		return ApiResult.ok(queryService.tree(warehouseId));
	}

	@GetMapping("/location/{id}")
	@Operation(summary = "单库位库存详情")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<LocationInventoryDetailVO> detail(@PathVariable("id") Long id) {
		return ApiResult.ok(queryService.detail(id));
	}

}
