package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.PurchaseOrderQcItem;
import com.erp.admin.wms.model.vo.PurchaseOrderQcItemVO;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;

/**
 * 质检数据Mapper
 *
 * @author erp
 */
public interface PurchaseOrderQcItemMapper extends ExtendMapper<PurchaseOrderQcItem> {

	/**
	 * 查询采购单质检数据列表（关联SKU和文件信息）
	 * @param purchaseOrderId 采购单ID
	 * @return 质检数据VO列表
	 */
	List<PurchaseOrderQcItemVO> selectVoListByPurchaseOrderId(@Param("purchaseOrderId") Long purchaseOrderId);

	/**
	 * 检查采购单+SKU编码是否已存在质检数据
	 * @param purchaseOrderId 采购单ID
	 * @param skuCode SKU编码
	 * @param excludeId 排除的ID（编辑时使用）
	 * @return 是否存在
	 */
	default boolean existsByOrderIdAndSkuCode(Long purchaseOrderId, String skuCode, Long excludeId) {
		LambdaQueryWrapperX<PurchaseOrderQcItem> wrapper = WrappersX.lambdaQueryX(PurchaseOrderQcItem.class)
				.eq(PurchaseOrderQcItem::getPurchaseOrderId, purchaseOrderId)
				.eq(PurchaseOrderQcItem::getSkuCode, skuCode)
				.neIfPresent(PurchaseOrderQcItem::getId, excludeId);
		return this.selectCount(wrapper) > 0;
	}

	/**
	 * 删除采购单的所有质检数据
	 * @param purchaseOrderId 采购单ID
	 * @return 删除数量
	 */
	default int deleteByPurchaseOrderId(Long purchaseOrderId) {
		LambdaQueryWrapperX<PurchaseOrderQcItem> wrapper = WrappersX.lambdaQueryX(PurchaseOrderQcItem.class)
				.eq(PurchaseOrderQcItem::getPurchaseOrderId, purchaseOrderId);
		return this.delete(wrapper);
	}

}
