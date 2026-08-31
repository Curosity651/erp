package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.entity.PurchaseOrder;
import com.erp.admin.wms.model.qo.PurchaseOrderQO;
import com.erp.admin.wms.model.vo.PurchaseOrderExportVO;
import com.erp.admin.wms.model.vo.PurchaseOrderPageVO;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;

/**
 * 采购单 Mapper
 *
 * @author erp
 */
public interface PurchaseOrderMapper extends ExtendMapper<PurchaseOrder> {

	/**
	 * 分页查询（含SKU筛选）
	 * @param page 分页对象
	 * @param qo 查询条件
	 * @return 分页结果
	 */
	IPage<PurchaseOrderPageVO> queryPage(IPage<PurchaseOrderPageVO> page, @Param("qo") PurchaseOrderQO qo);

	/**
	 * 导出查询
	 * @param qo 查询条件
	 * @return 导出数据列表
	 */
	List<PurchaseOrderExportVO> selectListForExport(@Param("qo") PurchaseOrderQO qo);

	/**
	 * 检查采购单号是否存在
	 * @param orderNo 采购单号
	 * @param excludeId 排除的ID
	 * @return 是否存在
	 */
	default boolean existsByOrderNo(String orderNo, Long excludeId) {
		LambdaQueryWrapperX<PurchaseOrder> wrapper = WrappersX.lambdaQueryX(PurchaseOrder.class)
				.eq(PurchaseOrder::getOrderNo, orderNo)
				.neIfPresent(PurchaseOrder::getId, excludeId);
		return this.selectCount(wrapper) > 0;
	}

	/**
	 * Check whether a purchase order is referenced by an active shipping order.
	 * @param purchaseOrderId purchase order ID
	 * @return whether an association exists
	 */
	boolean existsAssociatedShippingOrder(@Param("purchaseOrderId") Long purchaseOrderId);

}
