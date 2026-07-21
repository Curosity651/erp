package com.erp.admin.product.controller;

import java.util.List;

import com.erp.admin.product.model.entity.Supplier;
import com.erp.admin.product.model.qo.SupplierQO;
import com.erp.admin.product.model.vo.SupplierPageVO;
import com.erp.admin.product.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.common.model.result.BaseResultCode;
import org.ballcat.log.operation.annotation.OperationLog;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * 供应商
 *
 * @author ballcat 2025-07-26 22:36:14
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/product/supplier")
@Tag(name = "供应商管理")
public class SupplierController {

	private final SupplierService supplierService;

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param supplierQO 供应商查询对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "分页查询")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('product:supplier:read')")
	public ApiResult<PageResult<SupplierPageVO>> getSupplierPage(PageParam pageParam, SupplierQO supplierQO) {
		return ApiResult.ok(this.supplierService.queryPage(pageParam, supplierQO));
	}

	/**
	 * 新增供应商
	 * @param supplier 供应商
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "新增供应商")
	@OperationLog(bizType = "供应商", successMessage = "新增成功")
	@PostMapping
	@PreAuthorize("@per.hasPermission('product:supplier:add')")
	public ApiResult<Void> save(@RequestBody Supplier supplier) {
		return this.supplierService.save(supplier) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "新增供应商失败");
	}

	/**
	 * 修改供应商
	 * @param supplier 供应商
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "修改供应商")
	@OperationLog(bizType = "供应商", successMessage = "修改成功")
	@PutMapping
	@PreAuthorize("@per.hasPermission('product:supplier:edit')")
	public ApiResult<Void> updateById(@RequestBody Supplier supplier) {
		return this.supplierService.updateById(supplier) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "修改供应商失败");
	}

	/**
	 * 通过id删除供应商
	 * @param id id
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "通过id删除供应商")
	@OperationLog(bizType = "供应商", successMessage = "删除成功")
	@DeleteMapping("/{id}")
	@PreAuthorize("@per.hasPermission('product:supplier:del')")
	public ApiResult<Void> removeById(@PathVariable("id") Long id) {
		return this.supplierService.removeById(id) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "通过id删除供应商失败");
	}

	/**
	 * 获取供应商选项列表（用于下拉选择）
	 * @param keyword 搜索关键词（可选）
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取供应商选项列表")
	@GetMapping("/options")
	@PreAuthorize("@per.hasPermission('product:supplier:read')")
	public ApiResult<List<SupplierPageVO>> getSupplierOptions(
			@RequestParam(value = "keyword", required = false) String keyword) {
		return ApiResult.ok(this.supplierService.getSupplierOptions(keyword));
	}

	@Operation(summary = "Get business license preview URL")
	@GetMapping("/{id}/business-license-url")
	@PreAuthorize("@per.hasPermission('product:supplier:read')")
	public ApiResult<String> getBusinessLicenseUrl(@PathVariable("id") Long id) {
		return ApiResult.ok(this.supplierService.getBusinessLicenseUrl(id));
	}

}
