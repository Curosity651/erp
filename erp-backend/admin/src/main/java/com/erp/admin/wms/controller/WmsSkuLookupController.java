package com.erp.admin.wms.controller;

import java.util.List;

import com.erp.admin.wms.model.vo.SkuLookupVO;
import com.erp.admin.wms.service.WmsSkuLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 作业用 SKU 速查（数据级可见性 ②）。作用域随身份由 {@link WmsSkuLookupService} 控制。
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/sku-lookup")
@Tag(name = "作业SKU速查")
public class WmsSkuLookupController {

	private final WmsSkuLookupService skuLookupService;

	@Operation(summary = "按SKU编码速查商品(作用域随身份)")
	@GetMapping
	@PreAuthorize("@per.hasPermission('wms:inventory:read')")
	public ApiResult<List<SkuLookupVO>> lookup(@RequestParam("skuCode") String skuCode) {
		return ApiResult.ok(skuLookupService.lookup(skuCode));
	}

}
