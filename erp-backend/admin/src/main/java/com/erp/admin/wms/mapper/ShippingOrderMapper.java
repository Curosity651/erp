package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.entity.ShippingOrder;
import com.erp.admin.wms.model.qo.AvailableItemQO;
import com.erp.admin.wms.model.qo.AvailableShippingQO;
import com.erp.admin.wms.model.qo.ShippingOrderQO;
import com.erp.admin.wms.model.vo.AvailableItemVO;
import com.erp.admin.wms.model.vo.AvailableShippingVO;
import com.erp.admin.wms.model.vo.IncomingPlanVO;
import com.erp.admin.wms.model.vo.ShippingOrderDetailVO;
import com.erp.admin.wms.model.vo.ShippingOrderExportVO;
import com.erp.admin.wms.model.vo.ShippingOrderPageVO;
import com.erp.admin.wms.model.vo.ShippingOrderSimpleVO;
import com.erp.admin.wms.model.vo.ShippingOrderStatsVO;
import com.erp.admin.wms.model.vo.ShippingQuantityStatsVO;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.erp.admin.wms.model.vo.InTransitQuantityVO;

/**
 * 物流单 Mapper
 *
 * @author erp
 */
public interface ShippingOrderMapper extends ExtendMapper<ShippingOrder> {

	/**
	 * 分页查询
	 * @param page 分页对象
	 * @param qo 查询参数
	 * @return IPage<ShippingOrderPageVO> VO分页数据
	 */
	IPage<ShippingOrderPageVO> queryPage(IPage<ShippingOrderPageVO> page, @Param("qo") ShippingOrderQO qo);

	/**
	 * 批量查询物流单关联的采购单号
	 * @param ids 物流单ID列表
	 * @return List<ShippingOrderStatsVO> 统计信息列表
	 */
	List<ShippingOrderStatsVO> selectPurchaseOrderNosByIds(@Param("ids") Collection<Long> ids);

	/**
	 * 查询物流单详情
	 * @param id 物流单ID
	 * @return ShippingOrderDetailVO 物流单详情
	 */
	ShippingOrderDetailVO selectDetailById(@Param("id") Long id);

	/**
	 * 查询可发货采购明细
	 * @param qo 查询参数
	 * @return List<AvailableItemVO> 可发货采购明细列表
	 */
	List<AvailableItemVO> selectAvailableItems(@Param("qo") AvailableItemQO qo);


	/**
	 * 查询可入库物流单列表
	 * @param qo 查询参数
	 * @return List<AvailableShippingVO> 可入库物流单列表
	 */
	List<AvailableShippingVO> selectAvailableShipping(@Param("qo") AvailableShippingQO qo);

	/**
	 * 根据物流单号查询（用于唯一性校验）
	 * @param shippingNo 物流单号
	 * @param excludeId 排除的ID（编辑时使用）
	 * @return ShippingOrder 物流单
	 */
	ShippingOrder selectByShippingNo(@Param("shippingNo") String shippingNo, @Param("excludeId") Long excludeId);

	/**
	 * 查询导出列表
	 * @param qo 查询参数
	 * @return List<ShippingOrderExportVO> 导出数据列表
	 */
	List<ShippingOrderExportVO> selectListForExport(@Param("qo") ShippingOrderQO qo);

	/**
	 * 根据采购单ID查询关联物流单
	 * @param purchaseOrderId 采购单ID
	 * @return List<ShippingOrderSimpleVO> 关联物流单列表
	 */
	List<ShippingOrderSimpleVO> selectByPurchaseOrderId(@Param("purchaseOrderId") Long purchaseOrderId);

	/**
	 * 查询物流单发货数量统计
	 * @param shippingOrderId 物流单ID
	 * @return ShippingQuantityStatsVO 数量统计信息
	 */
	ShippingQuantityStatsVO selectShippingQuantityStats(@Param("shippingOrderId") Long shippingOrderId);

	/**
	 * 查询在途库存数量（按区域和SKU分组）
	 * <p>
	 * 查询已发货和部分到货状态的物流单，按区域+SKU汇总待入库数量
	 *
	 * @param regionIds 区域ID集合
	 * @param skuCodes SKU编码集合
	 * @param statuses 物流单状态列表
	 * @return 在途数量统计列表
	 */
	List<InTransitQuantityVO> selectInTransitQuantity(
			@Param("regionIds") Set<Long> regionIds,
			@Param("skuCodes") Set<String> skuCodes,
			@Param("statuses") List<String> statuses);

	/**
	 * 查询入库计划
	 * @param regionId 区域ID
	 * @param skuCode SKU编码
	 * @param statuses 物流单状态列表
	 * @return 入库计划列表
	 */
	List<IncomingPlanVO> selectIncomingPlan(
			@Param("regionId") Long regionId,
			@Param("skuCode") String skuCode,
			@Param("statuses") List<String> statuses);

}
