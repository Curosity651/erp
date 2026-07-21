package com.erp.admin.product.controller;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import com.erp.admin.product.exception.SkuBusinessException;
import com.erp.admin.product.model.dto.SkuCreateDTO;
import com.erp.admin.product.model.dto.SkuUpdateDTO;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.model.qo.SkuQO;
import com.erp.admin.product.model.qo.SkuSelectQO;
import com.erp.admin.product.model.vo.SkuExportVO;
import com.erp.admin.product.model.vo.SkuPageVO;
import com.erp.admin.product.model.vo.SkuSelectVO;
import com.erp.admin.product.service.SkuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.common.model.result.BaseResultCode;
import org.ballcat.fastexcel.annotation.ResponseExcel;
import org.ballcat.log.operation.annotation.OperationLog;
import org.ballcat.web.accesslog.annotation.AccessLoggingRule;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SKU管理
 *
 * @author ballcat 2025-07-27 02:02:06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/product/sku")
@Tag(name = "SKU管理")
public class SkuController {

	private final SkuService skuService;

	/**
	 * 分页查询SKU
	 * @param pageParam 分页参数
	 * @param skuQO SKU查询对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "分页查询SKU")
	@PostMapping("/page")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<PageResult<SkuPageVO>> getSkuPage(PageParam pageParam, @RequestBody SkuQO skuQO) {
		return ApiResult.ok(this.skuService.queryPage(pageParam, skuQO));
	}

	/**
	 * SKU选择弹窗分页查询（轻量级）
	 * @param pageParam 分页参数
	 * @param skuSelectQO SKU选择查询对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "SKU选择弹窗分页查询")
	@PostMapping("/select-page")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<PageResult<SkuSelectVO>> getSkuSelectPage(PageParam pageParam, @RequestBody SkuSelectQO skuSelectQO) {
		return ApiResult.ok(this.skuService.querySelectPage(pageParam, skuSelectQO));
	}

	/**
	 * 根据SKU编码批量查询（用于选择弹窗回显）
	 * @param skuCodes SKU编码列表，最多100条
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "根据SKU编码批量查询")
	@PostMapping("/list-by-codes")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<List<SkuSelectVO>> listByCodes(@RequestBody List<String> skuCodes) {
		return ApiResult.ok(this.skuService.listByCodes(skuCodes));
	}

	/**
	 * 导出SKU
	 * @param skuQO SKU查询对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "导出SKU")
	@PostMapping("/export")
	@AccessLoggingRule(includeQueryString = true, includeRequestBody = true)
	@ResponseExcel(name = "SKU导出_#{currentDateTime()}")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public List<SkuExportVO> exportSku(@RequestBody SkuQO skuQO) {
		return this.skuService.buildExportVOs(skuQO);
	}

	/**
	 * 新增SKU
	 * @param createDTO SKU创建DTO
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "新增SKU")
	@OperationLog(bizType = "SKU管理", successMessage = "新增成功")
	@PostMapping("/save")
	@PreAuthorize("@per.hasPermission('product:sku:add')")
	public ApiResult<Void> save(@Valid @RequestBody SkuCreateDTO createDTO) {
		try {
			// 1. 预检查：SKU编码是否已存在（更友好的提示，避免直接抛数据库约束异常）
			if (StringUtils.hasText(createDTO.getSkuCode())
					&& !this.skuService.validateSkuCode(createDTO.getSkuCode().trim().toUpperCase(), null)) {
				return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR,
						"SKU编码已存在: " + createDTO.getSkuCode().trim().toUpperCase());
			}
			boolean ok = this.skuService.saveSku(createDTO);
			return ok ? ApiResult.ok()
					: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "新增SKU失败，请检查必填字段及数据格式");
		}
		catch (DuplicateKeyException e) {
			String rootMsg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
			String message = "保存失败：SKU编码已存在";
			if (rootMsg != null) {
				rootMsg = rootMsg.toLowerCase();
				if (!(rootMsg.contains("sku") && rootMsg.contains("code"))) {
					message = "保存失败：唯一性冲突";
				}
			}
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, message);
		}
		catch (DataIntegrityViolationException e) {
			// 二次兜底：数据库唯一约束 (可能是 sku_code 或其他联合唯一)
			String rootMsg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, "保存失败：唯一性冲突" + (rootMsg != null ? (" - " + rootMsg) : ""));
		}
		catch (SkuBusinessException be) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, be.getMessage());
		}
		catch (Exception ex) {
			return ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "新增SKU异常：" + ex.getMessage());
		}
	}

	/**
	 * 修改SKU
	 * @param updateDTO SKU更新DTO
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "修改SKU")
	@OperationLog(bizType = "SKU管理", successMessage = "修改成功")
	@PostMapping("/update")
	@PreAuthorize("@per.hasPermission('product:sku:edit')")
	public ApiResult<Void> updateById(@Valid @RequestBody SkuUpdateDTO updateDTO) {
		try {
			// 预检查：SKU编码唯一性（排除自己）
			if (StringUtils.hasText(updateDTO.getSkuCode())
					&& !this.skuService.validateSkuCode(updateDTO.getSkuCode().trim().toUpperCase(), updateDTO.getId())) {
				return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR,
						"SKU编码已存在: " + updateDTO.getSkuCode().trim().toUpperCase());
			}
			boolean ok = this.skuService.updateSku(updateDTO);
			return ok ? ApiResult.ok()
					: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "修改SKU失败，请检查数据有效性");
		}
		catch (DuplicateKeyException e) {
			String rootMsg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
			String message = "修改失败：SKU编码已存在";
			if (rootMsg != null) {
				rootMsg = rootMsg.toLowerCase();
				if (!(rootMsg.contains("sku") && rootMsg.contains("code"))) {
					message = "修改失败：唯一性冲突";
				}
			}
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, message);
		}
		catch (DataIntegrityViolationException e) {
			String rootMsg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, "修改失败：唯一性冲突" + (rootMsg != null ? (" - " + rootMsg) : ""));
		}
		catch (SkuBusinessException be) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, be.getMessage());
		}
		catch (Exception ex) {
			return ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "修改SKU异常：" + ex.getMessage());
		}
	}

	/**
	 * 删除SKU
	 * @param id SKU ID
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "删除SKU")
	@OperationLog(bizType = "SKU管理", successMessage = "删除成功")
	@PostMapping("/delete")
	@PreAuthorize("@per.hasPermission('product:sku:del')")
	public ApiResult<Void> removeById(@Parameter(description = "SKU ID") @RequestParam Long id) {
		return this.skuService.remove(id) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "删除SKU失败");
	}

	/**
	 * 验证SKU编码唯一性
	 * @param code SKU编码
	 * @param excludeId 排除的ID（更新时使用）
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "验证SKU编码唯一性")
	@GetMapping("/validate-code")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<Boolean> validateSkuCode(@Parameter(description = "SKU编码") @RequestParam String code,
			@Parameter(description = "排除的ID（更新时使用）") @RequestParam(required = false) Long excludeId) {

		if (!StringUtils.hasText(code)) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, "SKU编码不能为空");
		}

		boolean isUnique = this.skuService.validateSkuCode(code, excludeId);
		return ApiResult.ok(isUnique);
	}

	/**
	 * 验证SKU序号唯一性
	 * @param skuNo SKU序号
	 * @param excludeId 排除的ID（更新时使用）
	 * @return ApiResult 通用返回体
	 * @deprecated SKU序号现在可以重复
	 */
	@Deprecated
	@Operation(summary = "验证SKU序号唯一性")
	@GetMapping("/validate-no")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<Boolean> validateSkuNo(@Parameter(description = "SKU序号") @RequestParam Integer skuNo,
			@Parameter(description = "排除的ID（更新时使用）") @RequestParam(required = false) Long excludeId) {

		if (skuNo == null || skuNo <= 0) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, "SKU序号不能为空或小于等于0");
		}

		boolean isUnique = this.skuService.validateSkuNo(skuNo, excludeId);
		return ApiResult.ok(isUnique);
	}

	/**
	 * 根据SKU编码查询SKU
	 * @param code SKU编码
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "根据SKU编码查询SKU")
	@GetMapping("/by-code")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<Sku> getSkuByCode(@Parameter(description = "SKU编码") @RequestParam String code) {
		if (!StringUtils.hasText(code)) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, "SKU编码不能为空");
		}

		Sku sku = this.skuService.getBySkuCode(code);
		if (sku == null) {
			throw SkuBusinessException.skuNotFound(code);
		}
		return ApiResult.ok(sku);
	}

	/**
	 * 根据SKU序号查询SKU
	 * @param skuNo SKU序号
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "根据SKU序号查询SKU")
	@GetMapping("/by-no")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<Sku> getSkuByNo(@Parameter(description = "SKU序号") @RequestParam Integer skuNo) {
		if (skuNo == null || skuNo <= 0) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, "SKU序号不能为空或小于等于0");
		}

		Sku sku = this.skuService.getBySkuNo(skuNo);
		if (sku == null) {
			throw SkuBusinessException.skuNotFound("SKU不存在，序号: " + skuNo);
		}
		return ApiResult.ok(sku);
	}

	/**
	 * 根据品类ID查询SKU列表
	 * @param categoryId 品类ID
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "根据品类ID查询SKU列表")
	@GetMapping("/by-category")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<List<Sku>> getSkuByCategoryId(@Parameter(description = "品类ID") @RequestParam Long categoryId) {
		List<Sku> skuList = this.skuService.getByCategoryId(categoryId);
		return ApiResult.ok(skuList);
	}

	/**
	 * 根据产品状态查询SKU列表
	 * @param productStatus 产品状态
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "根据产品状态查询SKU列表")
	@GetMapping("/by-status")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<List<Sku>> getSkuByProductStatus(
			@Parameter(description = "产品状态") @RequestParam Integer productStatus) {
		List<Sku> skuList = this.skuService.getByProductStatus(productStatus);
		return ApiResult.ok(skuList);
	}

	/**
	 * 根据供应商编码查询SKU列表
	 * @param supplierCode 供应商编码
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "根据供应商编码查询SKU列表")
	@GetMapping("/by-supplier")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<List<Sku>> getSkuBySupplierCode(
			@Parameter(description = "供应商编码") @RequestParam String supplierCode) {
		if (!StringUtils.hasText(supplierCode)) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, "供应商编码不能为空");
		}
		List<Sku> skuList = this.skuService.getBySupplierCode(supplierCode);
		return ApiResult.ok(skuList);
	}

	/**
	 * 根据品牌查询SKU列表
	 * @param brand 品牌
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "根据品牌查询SKU列表")
	@GetMapping("/by-brand")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<List<Sku>> getSkuByBrand(@Parameter(description = "品牌") @RequestParam String brand) {
		if (!StringUtils.hasText(brand)) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, "品牌不能为空");
		}
		List<Sku> skuList = this.skuService.getByBrand(brand);
		return ApiResult.ok(skuList);
	}

	/**
	 * 统计各品牌的SKU数量
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "统计各品牌的SKU数量")
	@GetMapping("/stats/by-brand")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<List<Map<String, Object>>> getSkuCountByBrand() {
		List<Map<String, Object>> stats = this.skuService.countByBrand();
		return ApiResult.ok(stats);
	}

	/**
	 * 统计各供应商的SKU数量
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "统计各供应商的SKU数量")
	@GetMapping("/stats/by-supplier")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<List<Map<String, Object>>> getSkuCountBySupplier() {
		List<Map<String, Object>> stats = this.skuService.countBySupplier();
		return ApiResult.ok(stats);
	}

	/**
	 * 获取所有品牌列表（去重）
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取所有品牌列表")
	@GetMapping("/brands")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<List<String>> getDistinctBrands() {
		return ApiResult.ok(this.skuService.getDistinctBrands());
	}

	/**
	 * 获取所有项目组列表（去重）
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取所有项目组列表")
	@GetMapping("/project-groups")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<List<String>> getDistinctProjectGroups() {
		return ApiResult.ok(this.skuService.getDistinctProjectGroups());
	}

	/**
	 * 获取所有供应商编码列表（去重）
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取所有供应商编码列表")
	@GetMapping("/supplier-codes")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<List<String>> getDistinctSupplierCodes() {
		return ApiResult.ok(this.skuService.getDistinctSupplierCodes());
	}

	/**
	 * 获取所有销售国家列表（去重）
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取所有销售国家列表")
	@GetMapping("/sales-countries")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<List<String>> getDistinctSalesCountries() {
		return ApiResult.ok(this.skuService.getDistinctSalesCountries());
	}

	/**
	 * 获取筛选选项数据
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取筛选选项数据")
	@GetMapping("/filter-options")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<Map<String, Object>> getFilterOptions() {
		return ApiResult.ok(this.skuService.getFilterOptions());
	}

	/**
	 * 获取SKU数据概览统计
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取SKU数据概览统计")
	@GetMapping("/overview-stats")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<Map<String, Object>> getSkuOverviewStats() {
		return ApiResult.ok(this.skuService.getSkuOverviewStats());
	}

	/**
	 * 批量更新产品状态
	 * @param skuIds SKU ID列表
	 * @param productStatus 新的产品状态
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "批量更新产品状态")
	@OperationLog(bizType = "SKU管理", successMessage = "批量更新产品状态成功")
	@PostMapping("/batch-update-status")
	@PreAuthorize("@per.hasPermission('product:sku:edit')")
	public ApiResult<Integer> batchUpdateProductStatus(
			@Parameter(description = "SKU ID列表") @RequestBody List<Long> skuIds,
			@Parameter(description = "新的产品状态") @RequestParam Integer productStatus) {

		if (skuIds == null || skuIds.isEmpty()) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, "SKU ID列表不能为空");
		}

		if (productStatus == null || productStatus < 1 || productStatus > 4) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, "产品状态值无效");
		}

		int successCount = this.skuService.batchUpdateProductStatus(skuIds, productStatus);
		return ApiResult.ok(successCount);
	}

	/**
	 * 导出SKU数据
	 * @param skuQO 查询条件
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "导出SKU数据")
	@GetMapping("/export")
	@PreAuthorize("@per.hasPermission('product:sku:export')")
	public ApiResult<List<Sku>> exportSkus(SkuQO skuQO) {
		List<Sku> skuList = this.skuService.exportSkus(skuQO);
		return ApiResult.ok(skuList);
	}

	/**
	 * 获取SKU详情（包含文件信息）
	 * @param id SKU ID
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取SKU详情")
	@GetMapping("/detail")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<com.erp.admin.product.model.vo.SkuDetailVO> getSkuDetail(
			@Parameter(description = "SKU ID") @RequestParam Long id) {
		com.erp.admin.product.model.vo.SkuDetailVO detailVO = this.skuService.getSkuDetailWithFiles(id);
		return detailVO != null ? ApiResult.ok(detailVO) : ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, "SKU不存在");
	}

	/**
	 * 获取SKU文件列表
	 * @param skuId SKU ID
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取SKU文件列表")
	@GetMapping("/files")
	@PreAuthorize("@per.hasPermission('product:sku:read')")
	public ApiResult<List<com.erp.admin.product.model.vo.SkuFileVO>> getSkuFiles(
			@Parameter(description = "SKU ID") @RequestParam Long skuId) {
		List<com.erp.admin.product.model.vo.SkuFileVO> files = this.skuService.getSkuFiles(skuId);
		return ApiResult.ok(files);
	}

	/**
	 * 删除SKU文件
	 * @param skuId SKU ID
	 * @param fileType 文件类型（可选）
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "删除SKU文件")
	@OperationLog(bizType = "SKU管理", successMessage = "删除文件成功")
	@PostMapping("/delete-files")
	@PreAuthorize("@per.hasPermission('product:sku:edit')")
	public ApiResult<Void> deleteSkuFiles(@Parameter(description = "SKU ID") @RequestParam Long skuId,
			@Parameter(description = "文件类型（可选，为空时删除所有文件）") @RequestParam(required = false) String fileType) {
		return this.skuService.deleteSkuFiles(skuId, fileType) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "删除SKU文件失败");
	}

}
