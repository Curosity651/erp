package com.erp.admin.order.service.label;

import com.erp.admin.order.service.LabelConstants;
import com.erp.admin.order.util.LabelUtils;
import com.erp.admin.product.model.entity.Category;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 面单文件名构建组件
 * <p>
 * 职责：
 * - 构建标准化的面单文件名
 * - SKU 编号提取规则（家具分类判断）
 * - 文件名安全化处理
 * <p>
 * 使用场景：WB 和 Ozon 面单打印服务
 *
 * @author system
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LabelFileNameBuilder {

	private final CategoryService categoryService;

	/**
	 * 构建文件名
	 *
	 * @param batchNo       批次号
	 * @param type          文件类型
	 * @param warehouseName 仓库名称
	 * @param sku           SKU对象
	 * @param count         数量
	 * @return 文件名
	 */
	public String buildFileName(String batchNo, String type, String warehouseName, Sku sku, int count) {
		String w = LabelUtils.sanitizeFileComponent(warehouseName);
		String skuCode = sku == null ? "{skuCode}" : sku.getSkuCode();
		String c = LabelUtils.sanitizeFileComponent(skuCode);
		String n = LabelUtils.sanitizeFileComponent(extractSkuNo(sku));

		String core = String.join("·", type, w, c, n, String.valueOf(count), batchNo);

		if (core.length() > LabelConstants.MAX_FILENAME_LENGTH) {
			core = core.substring(0, LabelConstants.MAX_FILENAME_LENGTH);
		}
		return core + ".pdf";
	}

	/**
	 * 提取 SKU 编号（根据分类判断）
	 * 家具分类显示真实 skuNo，其他分类显示 'x'
	 *
	 * @param sku SKU对象
	 * @return SKU编号字符串
	 */
	public String extractSkuNo(Sku sku) {
		if (sku == null) {
			return "{skuNo}";
		}

		// 判断是否为家具分类或其子分类
		if (isFurnitureOrSubCategory(sku)) {
			return sku.getSkuNo() == null ? "" : String.valueOf(sku.getSkuNo());
		}
		return "x";
	}

	/**
	 * 判断 SKU 是否属于家具分类或其子分类
	 *
	 * @param sku SKU对象
	 * @return 是否为家具分类
	 */
	private boolean isFurnitureOrSubCategory(Sku sku) {
		if (sku == null || sku.getCategoryId() == null) {
			return false;
		}

		Long categoryId = sku.getCategoryId();

		// 直接匹配家具分类
		if (Objects.equals(categoryId, LabelConstants.FURNITURE_CATEGORY_ID)) {
			return true;
		}

		// 沿父链判断是否属于家具的子分类
		List<Category> path = categoryService.getCategoryPath(categoryId);
		if (path != null) {
			for (Category category : path) {
				if (category != null && Objects.equals(category.getId(), LabelConstants.FURNITURE_CATEGORY_ID)) {
					return true;
				}
			}
		}
		return false;
	}
}
