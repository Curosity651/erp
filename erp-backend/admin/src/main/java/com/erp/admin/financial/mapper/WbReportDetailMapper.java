package com.erp.admin.financial.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.erp.admin.financial.converter.WbReportDetailConverter;
import com.erp.admin.financial.model.entity.WbReportDetail;
import com.erp.admin.financial.model.qo.WbReportDetailQO;
import com.erp.admin.financial.model.vo.WbReportDetailPageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * WB 财务报表明细 Mapper
 *
 * @author system
 */
@Mapper
public interface WbReportDetailMapper extends ExtendMapper<WbReportDetail> {

	DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * 分页查询（XML 实现，支持 LEFT JOIN 优化）
	 *
	 * @param page               分页对象
	 * @param ew                 查询条件 Wrapper
	 * @param unmatchedOrderOnly 是否仅查询未关联订单的记录
	 * @return 分页数据
	 */
	IPage<WbReportDetail> selectPageWithOrderMatch(
			IPage<WbReportDetail> page,
			@Param(Constants.WRAPPER) Wrapper<WbReportDetail> ew,
			@Param("unmatchedOrderOnly") Boolean unmatchedOrderOnly);

	/**
	 * 统计符合条件的记录总数（XML 实现）
	 *
	 * @param ew                   查询条件 Wrapper
	 * @return 记录总数
	 */
	Long selectCountWithOrderMatch(
			@Param(Constants.WRAPPER) Wrapper<WbReportDetail> ew
	);

	/**
	 * 导出查询（XML 实现，带限制条数）
	 *
	 * @param ew                   查询条件 Wrapper
	 * @param unmatchedOrderOnly 是否仅查询未关联订单的记录
	 * @param limit                最大返回条数
	 * @return 数据列表
	 */
	List<WbReportDetail> selectListWithOrderMatch(
			@Param(Constants.WRAPPER) Wrapper<WbReportDetail> ew,
			@Param("unmatchedOrderOnly") Boolean unmatchedOrderOnly,
			@Param("limit") int limit
	);

	/**
	 * 分页查询
	 *
	 * @param pageParam 分页参数
	 * @param qo        查询参数
	 * @return PageResult<WbReportDetailPageVO> VO分页数据
	 */
	default PageResult<WbReportDetailPageVO> queryPage(PageParam pageParam, WbReportDetailQO qo) {
		IPage<WbReportDetail> page = this.prodPage(pageParam);
		Boolean unmatchedOrderOnly = Boolean.TRUE.equals(qo.getUnmatchedOrderOnly()) ? Boolean.TRUE : null;
		LambdaQueryWrapperX<WbReportDetail> wrapper = this.buildQueryWrapper(qo, unmatchedOrderOnly);
		this.selectPageWithOrderMatch(page, wrapper, unmatchedOrderOnly);
		IPage<WbReportDetailPageVO> voPage = page.convert(WbReportDetailConverter.INSTANCE::toPageVO);
		return new PageResult<>(voPage.getRecords(), voPage.getTotal());
	}

	default List<WbReportDetail> selectListByQo(WbReportDetailQO qo, int limit) {
		Boolean unmatchedOrderOnly = Boolean.TRUE.equals(qo.getUnmatchedOrderOnly()) ? Boolean.TRUE : null;
		LambdaQueryWrapperX<WbReportDetail> wrapper = this.buildQueryWrapper(qo, unmatchedOrderOnly);
		return this.selectListWithOrderMatch(wrapper, unmatchedOrderOnly, limit);
	}

	/**
	 * 根据 periodType 和 rrdId 集合查询已存在的报表明细记录
	 *
	 * @param periodType 报表周期类型（weekly/daily）
	 * @param rrdIds     rrdId 集合
	 * @return 已存在的明细列表
	 */
	default List<WbReportDetail> selectByPeriodTypeAndRrdIds(String periodType, Collection<Long> rrdIds) {
		if (periodType == null || periodType.isEmpty() || rrdIds == null || rrdIds.isEmpty()) {
			return java.util.Collections.emptyList();
		}
		LambdaQueryWrapperX<WbReportDetail> wrapper = WrappersX.lambdaQueryX(WbReportDetail.class);
		wrapper.eq(WbReportDetail::getPeriodType, periodType);
		wrapper.in(WbReportDetail::getRrdId, rrdIds);
		return this.selectList(wrapper);
	}

	default LambdaQueryWrapperX<WbReportDetail> buildQueryWrapper(WbReportDetailQO qo, Boolean unmatchedOrderOnly) {
		LambdaQueryWrapperX<WbReportDetail> wrapper = WrappersX.lambdaAliasQueryX(WbReportDetail.class);

		if (qo.getShopId() != null) {
			wrapper.eq(WbReportDetail::getShopId, qo.getShopId());
		}

		if (qo.getPeriodType() != null && !qo.getPeriodType().isEmpty()) {
			wrapper.eq(WbReportDetail::getPeriodType, qo.getPeriodType());
		}

		if (qo.getRrDtStart() != null && !qo.getRrDtStart().isEmpty()) {
			wrapper.ge(WbReportDetail::getRrDt, LocalDate.parse(qo.getRrDtStart()));
		}

		if (qo.getRrDtEnd() != null && !qo.getRrDtEnd().isEmpty()) {
			wrapper.le(WbReportDetail::getRrDt, LocalDate.parse(qo.getRrDtEnd()));
		}

		if (qo.getOrderDtStart() != null && !qo.getOrderDtStart().isEmpty()) {
			wrapper.ge(WbReportDetail::getOrderDt, LocalDateTime.parse(qo.getOrderDtStart(), DATE_TIME_FORMATTER));
		}

		if (qo.getOrderDtEnd() != null && !qo.getOrderDtEnd().isEmpty()) {
			wrapper.le(WbReportDetail::getOrderDt, LocalDateTime.parse(qo.getOrderDtEnd(), DATE_TIME_FORMATTER));
		}

		if (qo.getSaleDtStart() != null && !qo.getSaleDtStart().isEmpty()) {
			wrapper.ge(WbReportDetail::getSaleDt, LocalDateTime.parse(qo.getSaleDtStart(), DATE_TIME_FORMATTER));
		}

		if (qo.getSaleDtEnd() != null && !qo.getSaleDtEnd().isEmpty()) {
			wrapper.le(WbReportDetail::getSaleDt, LocalDateTime.parse(qo.getSaleDtEnd(), DATE_TIME_FORMATTER));
		}

		if (qo.getSupplierOperName() != null && !qo.getSupplierOperName().isEmpty()) {
			wrapper.like(WbReportDetail::getSupplierOperName, qo.getSupplierOperName());
		}

		if (qo.getRealizationreportId() != null) {
			wrapper.eq(WbReportDetail::getRealizationreportId, qo.getRealizationreportId());
		}

		if (qo.getAssemblyId() != null && !qo.getAssemblyId().isEmpty()) {
			wrapper.eq(WbReportDetail::getAssemblyId, qo.getAssemblyId());
		}

		// 将未关联订单条件加入 Wrapper（通过 apply 拼接原生 SQL）
		if (Boolean.TRUE.equals(unmatchedOrderOnly)) {
			wrapper.apply("(wrd.assembly_id IS NULL OR wrd.assembly_id = 0 OR eo.id IS NULL)");
		}

		wrapper.orderByDesc(WbReportDetail::getRrDt, WbReportDetail::getId);
		return wrapper;
	}

}
