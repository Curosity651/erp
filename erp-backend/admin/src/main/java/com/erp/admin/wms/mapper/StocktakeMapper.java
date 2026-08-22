package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.wms.model.entity.StocktakeOrder;
import com.erp.admin.wms.model.enums.StocktakeStatus;
import com.erp.admin.wms.model.qo.StocktakeQO;
import com.erp.admin.wms.model.vo.StocktakeDetailVO;
import com.erp.admin.wms.model.vo.StocktakePageVO;
import com.erp.admin.wms.model.vo.StocktakeStatsVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

import java.util.Collection;
import java.util.List;

/**
 * 盘点单 Mapper
 *
 * @author erp
 */
public interface StocktakeMapper extends ExtendMapper<StocktakeOrder> {

	/**
	 * 分页查询
	 * @param page 分页对象
	 * @param qo 查询参数
	 * @return IPage<StocktakePageVO> VO分页数据
	 */
	IPage<StocktakePageVO> queryPage(IPage<StocktakePageVO> page, @Param("qo") StocktakeQO qo);

	/**
	 * 查询盘点单详情
	 * @param id 盘点单ID
	 * @return StocktakeDetailVO 盘点单详情
	 */
	StocktakeDetailVO selectDetailById(@Param("id") Long id);

	/**
	 * 查询今日盘点单数量（用于生成单号）
	 * @param prefix 单号前缀（如：ST20260110）
	 * @return 今日盘点单数量
	 */
	int countTodayOrders(@Param("prefix") String prefix);

	/**
	 * 检查仓库是否有进行中的盘点单
	 * @param warehouseId 仓库ID
	 * @return 进行中的盘点单数量
	 */
	default long countInProgressByWarehouse(Long warehouseId) {
		LambdaQueryWrapper<StocktakeOrder> queryWrapper = Wrappers.lambdaQuery(StocktakeOrder.class)
				.eq(StocktakeOrder::getWarehouseId, warehouseId)
				.in(StocktakeOrder::getOrderStatus, StocktakeStatus.COUNTING.name(),
						StocktakeStatus.REVIEWING.name(), StocktakeStatus.READY.name());
		return this.selectCount(queryWrapper);
	}

	/**
	 * 批量查询盘点单统计信息
	 * @param ids 盘点单ID列表
	 * @return List<StocktakeStatsVO> 统计信息列表
	 */
	List<StocktakeStatsVO> selectStatsByIds(@Param("ids") Collection<Long> ids);

	@Select("SELECT id FROM wms_warehouse WHERE id = #{warehouseId} AND deleted = 0 FOR UPDATE")
	Long lockWarehouse(@Param("warehouseId") Long warehouseId);

	@Select("SELECT * FROM wms_stocktake_order WHERE id = #{id} AND deleted = 0 FOR UPDATE")
	StocktakeOrder selectByIdForUpdate(@Param("id") Long id);

	@Update("UPDATE wms_stocktake_order SET order_status = #{targetStatus}, update_time = NOW() "
			+ "WHERE id = #{id} AND order_status = #{expectedStatus} AND deleted = 0")
	int casStatus(@Param("id") Long id, @Param("expectedStatus") String expectedStatus,
			@Param("targetStatus") String targetStatus);

	@Select("SELECT COUNT(*) FROM wms_stocktake_location_task t "
			+ "JOIN wms_stocktake_order o ON o.id = t.stocktake_order_id AND o.deleted = 0 "
			+ "WHERE t.warehouse_id = #{warehouseId} AND t.location_code = #{locationCode} "
			+ "AND o.order_status IN ('COUNTING','REVIEWING','READY')")
	int countActiveLocationFreeze(@Param("warehouseId") Long warehouseId,
			@Param("locationCode") String locationCode);

}
