package com.erp.admin.statistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.statistics.model.dto.DashboardQueryDTO;
import com.erp.admin.statistics.model.qo.SkuRankingQO;
import com.erp.admin.statistics.model.stat.ArticleSalesStat;
import com.erp.admin.statistics.model.stat.OrderStatusStat;
import com.erp.admin.statistics.model.stat.PlatformFulfillmentStat;
import com.erp.admin.statistics.model.stat.SalesTrendStat;
import com.erp.admin.statistics.model.vo.SkuRankingItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dashboard统计数据Mapper
 *
 * @author erp
 */
@Mapper
public interface DashboardMapper extends BaseMapper<ErpOrder> {

	/**
	 * 查询指定年月的销售总额（用于目标进度计算，不受额外筛选条件影响）
	 *
	 * @param year     年份
	 * @param month    月份
	 * @param platform 平台筛选
	 * @return 销售总额（卢布*100）
	 */
	BigDecimal sumTotalAmountRubByYearMonth(@Param("year") int year, @Param("month") int month,
			@Param("platform") String platform);

	/**
	 * 查询指定年份的销售总额（用于目标进度计算，不受额外筛选条件影响）
	 *
	 * @param year     年份
	 * @param platform 平台筛选
	 * @return 销售总额（卢布*100）
	 */
	BigDecimal sumTotalAmountRubByYear(@Param("year") int year, @Param("platform") String platform);

	/**
	 * 查询日期范围内的销售总额
	 *
	 * @param qo 查询参数
	 * @return 销售总额（卢布*100）
	 */
	BigDecimal sumTotalAmountRubByDateRange(@Param("qo") DashboardQueryDTO qo);

	/**
	 * 按平台和履约类型聚合销售数据
	 *
	 * @param qo 查询参数
	 * @return 聚合结果列表
	 */
	List<PlatformFulfillmentStat> aggregateByPlatformAndFulfillment(@Param("qo") DashboardQueryDTO qo);

	/**
	 * 按小时聚合销售趋势
	 *
	 * @param qo 查询参数
	 * @return 趋势数据列表
	 */
	List<SalesTrendStat> aggregateSalesByHour(@Param("qo") DashboardQueryDTO qo);

	/**
	 * 按天聚合销售趋势
	 *
	 * @param qo 查询参数
	 * @return 趋势数据列表
	 */
	List<SalesTrendStat> aggregateSalesByDay(@Param("qo") DashboardQueryDTO qo);

	/**
	 * 按订单状态聚合
	 *
	 * @param qo 查询参数
	 * @return 状态统计列表
	 */
	List<OrderStatusStat> aggregateByOrderStatus(@Param("qo") DashboardQueryDTO qo);

	/**
	 * 按article聚合销量
	 *
	 * @param qo 查询参数
	 * @return article销量列表
	 */
	List<ArticleSalesStat> aggregateSalesByArticle(@Param("qo") DashboardQueryDTO qo);

	/**
	 * 分页查询SKU销量排名
	 *
	 * @param page 分页参数
	 * @param qo   查询条件
	 * @param sortField 排序字段 (amount/quantity)
	 * @param sortOrder 排序方向 (asc/desc)
	 * @return 分页结果
	 */
	Page<SkuRankingItemVO> selectSkuRankingPage(@Param("page") IPage<SkuRankingItemVO> page,
			@Param("qo") SkuRankingQO qo,
			@Param("sortField") String sortField,
			@Param("sortOrder") String sortOrder);

	/**
	 * 查询SKU销量排名（不分页，用于导出）
	 *
	 * @param qo 查询条件
	 * @param sortField 排序字段 (amount/quantity)
	 * @param sortOrder 排序方向 (asc/desc)
	 * @return SKU排名列表
	 */
	List<SkuRankingItemVO> selectSkuRankingList(@Param("qo") SkuRankingQO qo,
			@Param("sortField") String sortField,
			@Param("sortOrder") String sortOrder);

}
