package com.erp.admin.product.controller;

import java.util.List;

import com.erp.admin.product.model.entity.Brand;
import com.erp.admin.product.model.qo.BrandQO;
import com.erp.admin.product.model.vo.BrandListVO;
import com.erp.admin.product.model.vo.BrandPageVO;
import com.erp.admin.product.service.BrandService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 品牌管理
 *
 * @author ballcat 2025-07-26 22:36:14
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/product/brand")
@Tag(name = "品牌管理管理")
public class BrandController {

	private final BrandService brandService;

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param brandQO 品牌管理查询对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "分页查询")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('product:brand:read')")
	public ApiResult<PageResult<BrandPageVO>> getBrandPage(PageParam pageParam, BrandQO brandQO) {
		return ApiResult.ok(this.brandService.queryPage(pageParam, brandQO));
	}

	/**
	 * 新增品牌管理
	 * @param brand 品牌管理
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "新增品牌管理")
	@OperationLog(bizType = "品牌管理", successMessage = "新增成功")
	@PostMapping
	@PreAuthorize("@per.hasPermission('product:brand:add')")
	public ApiResult<Void> save(@RequestBody Brand brand) {
		return this.brandService.save(brand) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "新增品牌管理失败");
	}

	/**
	 * 修改品牌管理
	 * @param brand 品牌管理
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "修改品牌管理")
	@OperationLog(bizType = "品牌管理", successMessage = "修改成功")
	@PutMapping
	@PreAuthorize("@per.hasPermission('product:brand:edit')")
	public ApiResult<Void> updateById(@RequestBody Brand brand) {
		return this.brandService.updateById(brand) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "修改品牌管理失败");
	}

	/**
	 * 获取品牌列表（用于下拉选择）
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取品牌列表")
	@GetMapping("/list")
	@PreAuthorize("@per.hasPermission('product:brand:read')")
	public ApiResult<List<BrandListVO>> getBrandList() {
		return ApiResult.ok(this.brandService.getBrandList());
	}

	/**
	 * 通过id删除品牌管理
	 * @param id id
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "通过id删除品牌管理")
	@OperationLog(bizType = "品牌管理", successMessage = "删除成功")
	@DeleteMapping("/{id}")
	@PreAuthorize("@per.hasPermission('product:brand:del')")
	public ApiResult<Void> removeById(@PathVariable("id") Long id) {
		return this.brandService.removeById(id) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "通过id删除品牌管理失败");
	}

}
