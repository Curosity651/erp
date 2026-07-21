package com.erp.admin.product.controller;

import java.util.List;

import com.erp.admin.product.model.entity.SkuMapping;
import com.erp.admin.product.model.qo.SkuMappingQO;
import com.erp.admin.product.model.vo.SkuMappingPageVO;
import com.erp.admin.product.service.SkuMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.common.model.result.BaseResultCode;
import org.ballcat.fastexcel.annotation.ResponseExcel;
import org.ballcat.web.accesslog.annotation.AccessLoggingRule;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * SKU映射
 *
 * @author erp 2025-09-22 21:32:36
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/product/sku-mapping")
@Tag(name = "SKU映射管理")
public class SkuMappingController {

	private final SkuMappingService skuMappingService;

	/**
	 * 分页查询
	 *
	 * @param pageParam    分页参数
	 * @param skuMappingQO SKU映射查询对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "分页查询")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('product:sku-mapping:read')")
	public ApiResult<PageResult<SkuMappingPageVO>> getSkuMappingPage(
			PageParam pageParam, SkuMappingQO skuMappingQO) {
		PageResult<SkuMappingPageVO> page = skuMappingService.queryPage(pageParam, skuMappingQO);
		return ApiResult.ok(page);
	}


	/**
	 * 新增SKU映射
	 *
	 * @param skuMapping SKU映射
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "新增SKU映射")
	@PostMapping
	@PreAuthorize("@per.hasPermission('product:sku-mapping:add')")
	public ApiResult<Void> save(@RequestBody SkuMapping skuMapping) {
		return skuMappingService.save(skuMapping) ?
				ApiResult.ok() : ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "新增SKU映射失败");
	}

	/**
	 * 修改SKU映射
	 *
	 * @param skuMapping SKU映射
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "修改SKU映射")
	@PutMapping
	@PreAuthorize("@per.hasPermission('product:sku-mapping:edit')")
	public ApiResult<Void> updateById(@RequestBody SkuMapping skuMapping) {
		return skuMappingService.updateById(skuMapping) ?
				ApiResult.ok() : ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "修改SKU映射失败");
	}

	/**
	 * 通过id删除SKU映射
	 *
	 * @param id id
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "通过id删除SKU映射")
	@DeleteMapping("/{id}")
	@PreAuthorize("@per.hasPermission('product:sku-mapping:del')")
	public ApiResult<Void> removeById(@PathVariable("id") Long id) {
		return skuMappingService.removeById(id) ?
				ApiResult.ok() : ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "通过id删除SKU映射失败");
	}

	/**
	 * 校验 (shopId, platformItemId) 唯一性
	 */
	@Operation(summary = "校验唯一性")
	@GetMapping("/validate")
	@PreAuthorize("@per.hasPermission('product:sku-mapping:read')")
	public ApiResult<Boolean> validateUnique(@Parameter(description = "平台商品ID") @RequestParam String platformItemId,
											 @Parameter(description = "排除的ID") @RequestParam(required = false) Long excludeId) {
		if (platformItemId == null || platformItemId.isEmpty()) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, "shopId 或 platformItemId 不能为空");
		}
		return ApiResult.ok(skuMappingService.validateUnique(platformItemId, excludeId));
	}

	/**
	 * 导出
	 */
	@Operation(summary = "导出")
	@GetMapping("/export")
	@AccessLoggingRule(includeQueryString = true, includeRequestBody = true)
	@ResponseExcel(name = "SKU映射-#{currentDateTime()}")
	@PreAuthorize("@per.hasPermission('product:sku-mapping:read')")
	public List<SkuMapping> export(SkuMappingQO qo) {
		return skuMappingService.listForExport(qo);
	}

	/**
	 * 根据SKU编码查询映射列表
	 *
	 * @param skuCode SKU编码
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "根据SKU编码查询映射列表")
	@GetMapping("/by-sku-code")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<List<SkuMapping>> getBySkuCode(
			@Parameter(description = "SKU编码") @RequestParam String skuCode) {
		if (!StringUtils.hasText(skuCode)) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, "SKU编码不能为空");
		}
		List<SkuMapping> mappings = skuMappingService.listBySkuCode(skuCode);
		return ApiResult.ok(mappings);
	}

	/**
	 * 快速创建映射
	 *
	 * @param skuMapping SKU映射对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "快速创建映射")
	@PostMapping("/quick-create")
	@PreAuthorize("@per.hasPermission('product:sku-mapping:add')")
	public ApiResult<Void> quickCreate(@RequestBody SkuMapping skuMapping) {
		// 验证必填字段
		if (!StringUtils.hasText(skuMapping.getSkuCode())) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, "SKU编码不能为空");
		}
		if (!StringUtils.hasText(skuMapping.getPlatformItemId())) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, "平台商品ID不能为空");
		}

		// 验证SKU编码是否存在
		boolean skuExists = skuMappingService.validateSkuExists(skuMapping.getSkuCode());
		if (!skuExists) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, 
					"SKU编码不存在: " + skuMapping.getSkuCode());
		}

		// 验证平台商品ID唯一性
		boolean isUnique = skuMappingService.validateUnique(
				skuMapping.getPlatformItemId(), null);
		if (!isUnique) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, 
					"该平台商品已存在映射");
		}

		// 创建映射
		boolean success = skuMappingService.save(skuMapping);
		return success ? ApiResult.ok() 
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "创建映射失败");
	}

}