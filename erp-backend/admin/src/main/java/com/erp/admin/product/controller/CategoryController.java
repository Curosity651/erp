package com.erp.admin.product.controller;

import java.util.List;

import com.erp.admin.product.model.entity.Category;
import com.erp.admin.product.model.qo.CategoryQO;
import com.erp.admin.product.model.vo.CategoryPageVO;
import com.erp.admin.product.service.CategoryService;
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
 * 商品品类
 *
 * @author ballcat 2025-07-26 22:36:14
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/product/category")
@Tag(name = "商品品类管理")
public class CategoryController {

	private final CategoryService categoryService;

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param categoryQO 商品品类查询对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "分页查询")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('product:category:read')")
	public ApiResult<PageResult<CategoryPageVO>> getCategoryPage(PageParam pageParam, CategoryQO categoryQO) {
		return ApiResult.ok(this.categoryService.queryPage(pageParam, categoryQO));
	}

	/**
	 * 查询所有商品品类（用于树形结构）
	 * @param categoryQO 商品品类查询对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "查询所有商品品类")
	@GetMapping("/list")
	@PreAuthorize("@per.hasPermission('product:category:read')")
	public ApiResult<List<CategoryPageVO>> getCategoryList(CategoryQO categoryQO) {
		return ApiResult.ok(this.categoryService.queryList(categoryQO));
	}

	/**
	 * 新增商品品类
	 * @param category 商品品类
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "新增商品品类")
	@OperationLog(bizType = "商品品类", successMessage = "新增成功")
	@PostMapping
	@PreAuthorize("@per.hasPermission('product:category:add')")
	public ApiResult<Void> save(@RequestBody Category category) {
		return this.categoryService.save(category) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "新增商品品类失败");
	}

	/**
	 * 修改商品品类
	 * @param category 商品品类
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "修改商品品类")
	@OperationLog(bizType = "商品品类", successMessage = "修改成功")
	@PutMapping
	@PreAuthorize("@per.hasPermission('product:category:edit')")
	public ApiResult<Void> updateById(@RequestBody Category category) {
		return this.categoryService.updateById(category) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "修改商品品类失败");
	}

	/**
	 * 通过id删除商品品类
	 * @param id id
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "通过id删除商品品类")
	@OperationLog(bizType = "商品品类", successMessage = "删除成功")
	@DeleteMapping("/{id}")
	@PreAuthorize("@per.hasPermission('product:category:del')")
	public ApiResult<Void> removeById(@PathVariable("id") Long id) {
		return this.categoryService.removeById(id) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "通过id删除商品品类失败");
	}

	/**
	 * 修正所有品类的层级
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "修正品类层级")
	@OperationLog(bizType = "商品品类", successMessage = "层级修正成功")
	@PostMapping("/fix-levels")
	@PreAuthorize("@per.hasPermission('product:category:edit')")
	public ApiResult<Void> fixLevels() {
		this.categoryService.fixAllLevels();
		return ApiResult.ok();
	}

	/**
	 * 测试层级计算
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "测试层级计算")
	@GetMapping("/test-level")
	@PreAuthorize("@per.hasPermission('product:category:read')")
	public ApiResult<String> testLevel() {
		// 创建一个测试品类
		Category testCategory = new Category();
		testCategory.setName("测试品类");
		testCategory.setCode("TEST_CATEGORY");
		testCategory.setParentId(1L); // 假设父品类ID为1
		testCategory.setSort(1);
		testCategory.setStatus(1);

		boolean result = this.categoryService.save(testCategory);
		return ApiResult
			.ok("测试完成，保存结果: " + result + ", 品类ID: " + testCategory.getId() + ", 层级: " + testCategory.getLevel());
	}

}
