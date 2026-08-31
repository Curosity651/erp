package com.erp.admin.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.erp.admin.order.model.vo.SkuDailySalesVO;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.order.model.dto.MonthlyAmountDTO;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.enums.ErpOrderStatusEnum;
import com.erp.admin.order.model.enums.OutboundStatus;
import com.erp.admin.order.model.qo.ErpOrderQO;
import com.erp.admin.order.model.qo.PendingOrderQO;
import com.erp.admin.wms.model.dto.PlatformSkuSalesDTO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * 订单主表
 *
 * @author erp 2025-09-27 23:16:08
 */
public interface ErpOrderMapper extends ExtendMapper<ErpOrder> {

	@InterceptorIgnore(tenantLine = "true")
	@Select("SELECT * FROM erp_order WHERE id = #{orderId} AND tenant_id = #{erpTenantId} FOR UPDATE")
	ErpOrder selectForFulfillmentSubmit(@Param("orderId") Long orderId,
			@Param("erpTenantId") Long erpTenantId);

	@InterceptorIgnore(tenantLine = "true")
	@Update("UPDATE erp_order SET warehouse_fulfillment_status = #{status}, "
			+ "fulfillment_order_id = COALESCE(#{fulfillmentOrderId}, fulfillment_order_id), update_time = NOW() "
			+ "WHERE id = #{orderId} AND tenant_id = #{erpTenantId}")
	int updateWarehouseFulfillmentStatus(@Param("orderId") Long orderId,
			@Param("erpTenantId") Long erpTenantId, @Param("status") String status,
			@Param("fulfillmentOrderId") Long fulfillmentOrderId);

	/**
	 * 通用订单分页查询（XML 动态 SQL）
	 */
	IPage<ErpOrder> queryPageByQo(IPage<ErpOrder> page, @Param("qo") ErpOrderQO qo);

	/**
	 * 待出库订单分页查询（XML 动态 SQL）
	 */
	IPage<ErpOrder> queryPendingOutboundPage(IPage<ErpOrder> page, @Param("qo") PendingOrderQO qo);

	/**
	 * 分页查询订单实体（推荐使用）
	 * <p>
	 * 返回完整的 ErpOrderEntity，包含所有字段（特别是 rawJson）
	 * 由 Service 层负责转换为具体的平台 VO
	 *
	 * @param pageParam 分页参数
	 * @param qo        查询条件
	 * @return PageResult<ErpOrderEntity> 完整的订单实体分页数据
	 */
	default PageResult<ErpOrder> queryPageEntities(PageParam pageParam, ErpOrderQO qo) {
		IPage<ErpOrder> page = this.prodPage(pageParam);
		IPage<ErpOrder> result = this.queryPageByQo(page, qo);
		return new PageResult<>(result.getRecords(), result.getTotal());
	}

	/**
	 * 批量按主键查询
	 */
	default java.util.List<ErpOrder> selectByIds(java.util.Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) return java.util.Collections.emptyList();
		LambdaQueryWrapperX<ErpOrder> wrapper = WrappersX.lambdaQueryX(ErpOrder.class)
				.in(ErpOrder::getId, ids);
		return this.selectList(wrapper);
	}

	/**
	 * 按店铺、平台、平台订单号查询单条
	 */
	default ErpOrder selectOneByShopPlatformPlatformOrderId(Long shopId, String platform, String platformOrderId) {
		LambdaQueryWrapperX<ErpOrder> wrapper = WrappersX.lambdaQueryX(ErpOrder.class)
				.eq(ErpOrder::getShopId, shopId)
				.eq(ErpOrder::getPlatform, platform)
				.eq(ErpOrder::getPlatformOrderId, platformOrderId);
		return this.selectOne(wrapper);
	}

	/**
	 * 分批查询导出数据（XML 动态 SQL，复用 orderFilterClause）
	 *
	 * @param qo     查询条件
	 * @param offset 偏移量
	 * @param limit  每页大小
	 * @return 订单实体列表
	 */
	List<ErpOrder> selectExportBatch(@Param("qo") ErpOrderQO qo,
									 @Param("offset") int offset,
									 @Param("limit") int limit);

	/**
	 * 计算指定年份按月分组的已完成订单销售额(RUB)
	 * <p>
	 * 查询条件:
	 * - YEAR(platformCreatedAt) = year
	 * - erpStatus = 'DELIVERED'
	 * <p>
	 * 金额字段: 使用 totalAmountRub / 100
	 *
	 * @param year 目标年份
	 * @return 月度金额列表
	 */
	List<MonthlyAmountDTO> calculateMonthlyCompletedOrdersAmount(Integer year);

	/**
	 * 查询指定平台的未完结订单（erpStatus 为 READY_TO_SHIP 或 SHIPPED）
	 *
	 * @param platform 平台代码（如 "yandex"）
	 * @return 未完结订单列表
	 */
	default List<ErpOrder> selectUnfinishedByPlatform(String platform) {
		LambdaQueryWrapperX<ErpOrder> wrapper = WrappersX.lambdaQueryX(ErpOrder.class)
				.eq(ErpOrder::getPlatform, platform)
				.in(ErpOrder::getErpStatus, ErpOrderStatusEnum.READY_TO_SHIP.name(),
						ErpOrderStatusEnum.SHIPPED.name(), ErpOrderStatusEnum.ARRIVED_AT_PLATFORM_WAREHOUSE.name())
				.last("limit 1000");
		return this.selectList(wrapper);
	}

	/**
	 * 查询待出库订单列表
	 * <p>
	 * 查询条件：
	 * - platform = 指定平台
	 * - erpStatus = 'READY_TO_SHIP'
	 * - outboundStatus = 'NONE'
	 * - fulfillmentType = 'FBS'
	 *
	 * @param platform 平台（必填）
	 * @return 待出库订单列表
	 */
	default List<ErpOrder> selectPendingOutboundOrders(String platform) {
		LambdaQueryWrapperX<ErpOrder> wrapper = WrappersX.lambdaQueryX(ErpOrder.class)
				.eq(ErpOrder::getPlatform, platform)
				.eq(ErpOrder::getErpStatus, ErpOrderStatusEnum.SHIPPED)
				.eq(ErpOrder::getOutboundStatus, OutboundStatus.NONE.name())
				.isNull(ErpOrder::getFulfillmentOrderId)
				.isNull(ErpOrder::getOutboundOrderId)
				.eq(ErpOrder::getFulfillmentType, "fbs")
				.orderByDesc(ErpOrder::getPlatformCreatedAt, ErpOrder::getId);
		return this.selectList(wrapper);
	}

	/**
	 * 分页查询待出库订单列表
	 * <p>
	 * 查询条件：
	 * - platform = 指定平台
	 * - erpStatus = 'SHIPPED'
	 * - outboundStatus = 'NONE'
	 * - fulfillmentType = 'fbs'
	 * - locked = 0（未锁定）
	 * - keyword: 模糊匹配 platformOrderId（可选）
	 *
	 * @param pageParam 分页参数
	 * @param qo 查询条件
	 * @return 分页结果
	 */
	default PageResult<ErpOrder> selectPendingOutboundOrdersPage(PageParam pageParam, PendingOrderQO qo) {
		IPage<ErpOrder> page = this.prodPage(pageParam);
		IPage<ErpOrder> result = this.queryPendingOutboundPage(page, qo);
		return new PageResult<>(result.getRecords(), result.getTotal());
	}

	/**
	 * 更新订单出库状态
	 *
	 * @param id 订单ID
	 * @param outboundStatus 出库状态
	 * @param outboundOrderId 出库单ID
	 * @param outboundTime 出库时间
	 * @return 影响行数
	 */
	default int updateOutboundStatus(Long id, String outboundStatus, Long outboundOrderId, java.time.LocalDateTime outboundTime) {
		ErpOrder order = new ErpOrder();
		order.setId(id);
		order.setOutboundStatus(outboundStatus);
		order.setOutboundOrderId(outboundOrderId);
		order.setOutboundTime(outboundTime);
		return this.updateById(order);
	}

	/**
	 * 批量更新订单出库状态
	 *
	 * @param ids 订单ID列表
	 * @param outboundStatus 出库状态
	 * @param outboundOrderId 出库单ID
	 * @param outboundTime 出库时间
	 * @return 影响行数
	 */
	default int batchUpdateOutboundStatus(List<Long> ids, String outboundStatus, Long outboundOrderId, java.time.LocalDateTime outboundTime) {
		if (ids == null || ids.isEmpty()) return 0;
		ErpOrder order = new ErpOrder();
		order.setOutboundStatus(outboundStatus);
		order.setOutboundOrderId(outboundOrderId);
		order.setOutboundTime(outboundTime);
		return this.update(order, Wrappers.lambdaUpdate(ErpOrder.class).in(ErpOrder::getId, ids));
	}

	/**
	 * 使用乐观锁占用订单用于出库
	 * <p>
	 * 将 outboundStatus 从 NONE 更新为 ALLOCATED，同时 version + 1
	 *
	 * @param id 订单ID
	 * @param version 当前版本号
	 * @return 影响行数（0表示并发冲突）
	 */
	int allocateForOutboundWithVersion(@Param("id") Long id, @Param("version") Integer version);

	/**
	 * 释放订单出库占用
	 * <p>
	 * 将 outboundStatus 从 ALLOCATED 更新为 NONE
	 *
	 * @param ids 订单ID列表
	 * @param outboundOrderId 当前占用所属的销售出库单ID
	 * @return 影响行数
	 */
	int releaseOutboundAllocation(@Param("ids") List<Long> ids,
			@Param("outboundOrderId") Long outboundOrderId);

	/** 将已占用订单绑定到销售出库单，状态仍保持 ALLOCATED。 */
	int bindOutboundWithVersion(@Param("id") Long id, @Param("outboundOrderId") Long outboundOrderId,
			@Param("version") Integer version);

	/** 海外仓签出后将订单从 ALLOCATED 推进到 COMPLETED。 */
	@InterceptorIgnore(tenantLine = "true")
	int completeOutboundWithVersion(@Param("id") Long id, @Param("outboundOrderId") Long outboundOrderId,
			@Param("version") Integer version);

	@InterceptorIgnore(tenantLine = "true")
	@Select("SELECT * FROM erp_order WHERE id = #{id} AND tenant_id = #{tenantId}")
	ErpOrder selectForWarehouseComplete(@Param("id") Long id, @Param("tenantId") Long tenantId);

	/** 原子取得平台确认执行权；处理中超过十分钟的请求允许恢复。 */
	@Update("UPDATE erp_order SET confirm_state = 'PROCESSING', confirm_started_at = NOW(), update_time = NOW() " +
			"WHERE id = #{id} AND (confirm_state IS NULL OR confirm_state IN ('NONE','FAILED') " +
			"OR (confirm_state = 'PROCESSING' AND confirm_started_at < DATE_SUB(NOW(), INTERVAL 10 MINUTE)))")
	int claimPlatformConfirm(@Param("id") Long id);

	@Update("UPDATE erp_order SET confirm_state = #{state}, update_time = NOW() " +
			"WHERE id = #{id} AND confirm_state = 'PROCESSING'")
	int finishPlatformConfirm(@Param("id") Long id, @Param("state") String state);

	/**
	 * 更新订单已退货数量
	 * <p>
	 * 累加已退货数量
	 *
	 * @param orderId 订单ID
	 * @param quantity 退货数量（增量）
	 * @return 影响行数
	 */
	@Update("UPDATE erp_order " +
			"SET returned_quantity = COALESCE(returned_quantity, 0) + #{quantity} " +
			"WHERE id = #{orderId} " +
			"AND COALESCE(returned_quantity, 0) + #{quantity} <= total_quantity")
	int updateReturnedQuantity(@Param("orderId") Long orderId, @Param("quantity") Integer quantity);

	/**
	 * 按SKU编码批量查询销量统计
	 * <p>
	 * 统计已完成订单的销量，用于计算日均销量
	 *
	 * @param skuCodes SKU编码集合
	 * @param startTime 统计起始时间
	 * @return 销量统计列表
	 */
	List<SkuDailySalesVO> selectDailySalesBySkuCodes(
			@Param("skuCodes") Set<String> skuCodes,
			@Param("startTime") LocalDateTime startTime);

	/**
	 * 查询单个SKU的总销量
	 * @param skuCode SKU编码
	 * @param startTime 统计起始时间
	 * @return 总销量
	 */
	Long selectTotalSalesBySkuCode(
			@Param("skuCode") String skuCode,
			@Param("startTime") LocalDateTime startTime);

	/**
	 * 按平台和 SKU 统计 FBS 订单销量
	 */
	List<PlatformSkuSalesDTO> selectFbsSalesByPlatformsAndSkus(
			@Param("platforms") Collection<String> platforms,
			@Param("skuCodes") Collection<String> skuCodes,
			@Param("startTime") LocalDateTime startTime);

	/**
	 * 查询指定平台、SKU 在指定时间范围内的 FBS 销量
	 */
	List<PlatformSkuSalesDTO> selectFbsRecentSales(
			@Param("platforms") Collection<String> platforms,
			@Param("skuCodes") Collection<String> skuCodes,
			@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime);

	/**
	 * 查询 SKU 的首次 FBS 出单时间（按平台+SKU 维度）
	 */
	List<PlatformSkuSalesDTO> selectFirstFbsOrderTime(
			@Param("platforms") Collection<String> platforms,
			@Param("skuCodes") Collection<String> skuCodes);
}
