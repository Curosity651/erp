package com.erp.admin.wms.controller;

import com.erp.admin.wms.model.dto.StocktakeAddSkuDTO;
import com.erp.admin.wms.model.dto.StocktakeDTO;
import com.erp.admin.wms.model.dto.StocktakeItemsDTO;
import com.erp.admin.wms.model.dto.StocktakeExtraItemDTO;
import com.erp.admin.wms.model.qo.StocktakeQO;
import com.erp.admin.wms.model.vo.AvailableSkuPreviewVO;
import com.erp.admin.wms.model.vo.AvailableSkuVO;
import com.erp.admin.wms.model.vo.StocktakeDetailVO;
import com.erp.admin.wms.model.vo.StocktakeDiffPreviewVO;
import com.erp.admin.wms.model.vo.StocktakeItemVO;
import com.erp.admin.wms.model.vo.StocktakePageVO;
import com.erp.admin.wms.model.vo.StocktakeProgressVO;
import com.erp.admin.wms.model.vo.StocktakeLocationTaskVO;
import com.erp.admin.wms.model.vo.PalletSummaryVO;
import com.erp.admin.wms.service.StocktakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.validation.group.CreateGroup;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.log.operation.annotation.OperationLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.groups.Default;
import java.util.List;

/**
 * 盘点单管理
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/stocktake")
@Tag(name = "盘点单管理")
public class StocktakeController {

	private final StocktakeService stocktakeService;

	/**
	 * 分页查询
	 * 
	 * @param pageParam 分页参数
	 * @param qo        查询条件
	 * @return 分页结果
	 */
	@Operation(summary = "分页查询")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('wms:stocktake:read')")
	public ApiResult<PageResult<StocktakePageVO>> getPage(PageParam pageParam, StocktakeQO qo) {
		return ApiResult.ok(stocktakeService.queryPage(pageParam, qo));
	}

	/**
	 * 获取详情
	 * 
	 * @param id 盘点单ID
	 * @return 盘点单详情
	 */
	@Operation(summary = "获取详情")
	@GetMapping("/detail")
	@PreAuthorize("@per.hasPermission('wms:stocktake:read')")
	public ApiResult<StocktakeDetailVO> getDetail(@RequestParam Long id) {
		return ApiResult.ok(stocktakeService.getDetail(id));
	}

	/**
	 * 新建盘点单
	 * 
	 * @param dto 盘点单DTO
	 * @return 盘点单ID
	 */
	@Operation(summary = "新建盘点单")
	@OperationLog(bizType = "盘点单管理", successMessage = "新建成功")
	@PostMapping
	@PreAuthorize("@per.hasPermission('wms:stocktake:add')")
	public ApiResult<Long> create(@Validated({Default.class, CreateGroup.class}) @RequestBody StocktakeDTO dto) {
		return ApiResult.ok(stocktakeService.create(dto));
	}

	/**
	 * 获取盘点明细
	 * 
	 * @param id 盘点单ID
	 * @return 盘点明细列表
	 */
	@Operation(summary = "获取盘点明细")
	@GetMapping("/items")
	@PreAuthorize("@per.hasPermission('wms:stocktake:read')")
	public ApiResult<List<StocktakeItemVO>> getItems(@RequestParam Long id) {
		return ApiResult.ok(stocktakeService.getItems(id));
	}

	/**
	 * 获取盘点进度
	 * 
	 * @param id 盘点单ID
	 * @return 盘点进度
	 */
	@Operation(summary = "获取盘点进度")
	@GetMapping("/progress")
	@PreAuthorize("@per.hasPermission('wms:stocktake:read')")
	public ApiResult<StocktakeProgressVO> getProgress(@RequestParam Long id) {
		return ApiResult.ok(stocktakeService.getProgress(id));
	}

	@Operation(summary = "获取盘点库位任务")
	@GetMapping("/tasks")
	@PreAuthorize("@per.hasPermission('wms:stocktake:read')")
	public ApiResult<List<StocktakeLocationTaskVO>> getTasks(@RequestParam Long id) {
		return ApiResult.ok(stocktakeService.getTasks(id));
	}

	@Operation(summary = "获取库位任务明细")
	@GetMapping("/task/items")
	@PreAuthorize("@per.hasPermission('wms:stocktake:read')")
	public ApiResult<List<StocktakeItemVO>> getTaskItems(@RequestParam Long taskId) {
		return ApiResult.ok(stocktakeService.getTaskItems(taskId));
	}

	@Operation(summary = "获取当前库位可用货主")
	@GetMapping("/task/eligible-owners")
	@PreAuthorize("@per.hasPermission('wms:stocktake:read')")
	public ApiResult<List<Long>> getEligibleOwnerIds(@RequestParam Long taskId) {
		return ApiResult.ok(stocktakeService.getEligibleOwnerIds(taskId));
	}

	@Operation(summary = "新增账外商品")
	@PostMapping("/task/extra")
	@PreAuthorize("@per.hasPermission('wms:stocktake:input')")
	public ApiResult<StocktakeItemVO> addExtraItem(@Validated @RequestBody StocktakeExtraItemDTO dto) {
		return ApiResult.ok(stocktakeService.addExtraItem(dto));
	}

	@Operation(summary = "完成库位盘点")
	@PatchMapping("/task/complete")
	@PreAuthorize("@per.hasPermission('wms:stocktake:input')")
	public ApiResult<Void> completeTask(@RequestParam Long taskId) {
		stocktakeService.completeTask(taskId);
		return ApiResult.ok();
	}

	@Operation(summary = "提交差异复核")
	@PatchMapping("/review")
	@PreAuthorize("@per.hasPermission('wms:stocktake:confirm')")
	public ApiResult<Void> submitReview(@RequestParam Long id) {
		stocktakeService.submitReview(id);
		return ApiResult.ok();
	}

	/**
	 * 录入盘点数据
	 * 
	 * @param dto 盘点录入DTO
	 * @return 操作结果
	 */
	@Operation(summary = "录入盘点数据")
	@OperationLog(bizType = "盘点单管理", successMessage = "录入成功")
	@PutMapping("/items")
	@PreAuthorize("@per.hasPermission('wms:stocktake:input')")
	public ApiResult<Void> saveItems(@Validated @RequestBody StocktakeItemsDTO dto) {
		stocktakeService.saveItems(dto);
		return ApiResult.ok();
	}

	/**
	 * 确认盘点
	 * 
	 * @param id 盘点单ID
	 * @return 操作结果
	 */
	@Operation(summary = "确认盘点")
	@OperationLog(bizType = "盘点单管理", successMessage = "确认盘点成功")
	@PatchMapping("/confirm")
	@PreAuthorize("@per.hasPermission('wms:stocktake:confirm')")
	public ApiResult<Void> confirm(@RequestParam Long id) {
		stocktakeService.confirm(id);
		return ApiResult.ok();
	}

	@Operation(summary = "查询盘点后需要重新打印的托盘标签")
	@GetMapping("/pallets")
	@PreAuthorize("@per.hasPermission('wms:stocktake:read')")
	public ApiResult<List<PalletSummaryVO>> pallets(@RequestParam Long id) {
		return ApiResult.ok(stocktakeService.listPrintablePallets(id));
	}

	/**
	 * 取消盘点
	 * 
	 * @param id 盘点单ID
	 * @return 操作结果
	 */
	@Operation(summary = "取消盘点")
	@OperationLog(bizType = "盘点单管理", successMessage = "取消成功")
	@PatchMapping("/cancel")
	@PreAuthorize("@per.hasPermission('wms:stocktake:cancel')")
	public ApiResult<Void> cancel(@RequestParam Long id) {
		stocktakeService.cancel(id);
		return ApiResult.ok();
	}

	/**
	 * 获取差异预览（完成盘点前调用）
	 * 
	 * @param id 盘点单ID
	 * @return 差异预览
	 */
	@Operation(summary = "获取差异预览")
	@GetMapping("/diff-preview")
	@PreAuthorize("@per.hasPermission('wms:stocktake:read')")
	public ApiResult<StocktakeDiffPreviewVO> getDiffPreview(@RequestParam Long id) {
		return ApiResult.ok(stocktakeService.getDiffPreview(id));
	}

	/**
	 * 获取仓库可盘点SKU列表（预览）
	 * 
	 * @param warehouseId 仓库ID
	 * @return SKU预览
	 */
	@Operation(summary = "获取仓库可盘点SKU")
	@GetMapping("/available-skus")
	@PreAuthorize("@per.hasPermission('wms:stocktake:read')")
	public ApiResult<AvailableSkuPreviewVO> getAvailableSkus(@RequestParam Long warehouseId) {
		return ApiResult.ok(stocktakeService.getAvailableSkuPreview(warehouseId));
	}

	/**
	 * 批量标记无差异
	 * 
	 * @param id 盘点单ID
	 * @return 操作结果
	 */
	@Operation(summary = "批量标记无差异")
	@OperationLog(bizType = "盘点单管理", successMessage = "批量标记无差异成功")
	@PutMapping("/batch-no-diff")
	@PreAuthorize("@per.hasPermission('wms:stocktake:input')")
	public ApiResult<Void> batchMarkNoDiff(@RequestParam Long id) {
		stocktakeService.batchMarkNoDiff(id);
		return ApiResult.ok();
	}

	/**
	 * 删除盘点单
	 * 
	 * @param ids 盘点单ID列表
	 * @return 操作结果
	 */
	@Operation(summary = "删除盘点单")
	@OperationLog(bizType = "盘点单管理", successMessage = "删除成功")
	@DeleteMapping
	@PreAuthorize("@per.hasPermission('wms:stocktake:del')")
	public ApiResult<Void> delete(@RequestBody List<Long> ids) {
		stocktakeService.delete(ids);
		return ApiResult.ok();
	}

	/**
	 * 追加盘点SKU
	 *
	 * @param dto 追加请求
	 * @return 新增的明细列表
	 */
	@Operation(summary = "追加盘点SKU")
	@OperationLog(bizType = "盘点单管理", successMessage = "追加SKU成功")
	@PostMapping("/add-skus")
	@PreAuthorize("@per.hasPermission('wms:stocktake:input')")
	public ApiResult<List<StocktakeItemVO>> addSkus(@Validated @RequestBody StocktakeAddSkuDTO dto) {
		return ApiResult.ok(stocktakeService.addSkus(dto));
	}

	/**
	 * 删除盘点明细
	 *
	 * @param stocktakeId 盘点单ID
	 * @param itemIds     明细ID列表
	 * @return 操作结果
	 */
	@Operation(summary = "删除盘点明细")
	@OperationLog(bizType = "盘点单管理", successMessage = "删除明细成功")
	@DeleteMapping("/items")
	@PreAuthorize("@per.hasPermission('wms:stocktake:input')")
	public ApiResult<Void> removeItems(@RequestParam Long stocktakeId, @RequestBody List<Long> itemIds) {
		stocktakeService.removeItems(stocktakeId, itemIds);
		return ApiResult.ok();
	}

	/**
	 * 获取仓库内可选择的SKU（用于部分盘点）
	 *
	 * @param warehouseId 仓库ID
	 * @param stocktakeId 盘点单ID（用于排除已添加的）
	 * @return SKU列表
	 */
	@Operation(summary = "获取仓库内可选择的SKU")
	@GetMapping("/selectable-skus")
	@PreAuthorize("@per.hasPermission('wms:stocktake:read')")
	public ApiResult<List<AvailableSkuVO>> getSelectableSkus(
			@RequestParam Long warehouseId,
			@RequestParam(required = false) Long stocktakeId,
			@RequestParam(required = false) Long erpTenantId) {
		return ApiResult.ok(stocktakeService.getSelectableSkus(warehouseId, stocktakeId, erpTenantId));
	}

}
