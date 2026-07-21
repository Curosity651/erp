package com.erp.admin.statistics.mapper;

import java.util.List;

import com.erp.admin.statistics.model.SalesTarget;
import com.erp.admin.statistics.model.entity.enums.TargetType;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * 销售目标
 *
 * @author erp 2025-10-25 21:40:57
 */
public interface SalesTargetMapper extends ExtendMapper<SalesTarget> {

	/**
	 * 根据年份和目标类型查询
	 *
	 * @param targetYear 目标年份
	 * @param targetType 目标类型
	 * @return 销售目标列表
	 */
	default List<SalesTarget> selectByTargetYearAndTargetType(Integer targetYear, TargetType targetType) {
		LambdaQueryWrapperX<SalesTarget> wrapper = WrappersX.lambdaQueryX(SalesTarget.class)
				.eq(SalesTarget::getTargetYear, targetYear)
				.eq(SalesTarget::getTargetType, targetType);
		return this.selectList(wrapper);
	}

	/**
	 * 根据年份和月份查询月度目标
	 *
	 * @param targetYear  目标年份
	 * @param targetMonth 目标月份
	 * @return 销售目标
	 */
	default SalesTarget selectByTargetYearAndTargetMonth(Integer targetYear, Integer targetMonth) {
		LambdaQueryWrapperX<SalesTarget> wrapper = WrappersX.lambdaQueryX(SalesTarget.class)
				.eq(SalesTarget::getTargetYear, targetYear)
				.eq(SalesTarget::getTargetType, TargetType.MONTHLY)
				.eq(SalesTarget::getTargetMonth, targetMonth);
		return this.selectOne(wrapper);
	}

	/**
	 * 检查年度目标是否已存在
	 *
	 * @param targetYear 目标年份
	 * @return 是否存在
	 */
	default boolean existsYearlyTarget(Integer targetYear) {
		LambdaQueryWrapperX<SalesTarget> wrapper = WrappersX.lambdaQueryX(SalesTarget.class)
				.eq(SalesTarget::getTargetYear, targetYear)
				.eq(SalesTarget::getTargetType, TargetType.YEARLY);
		return this.selectCount(wrapper) > 0;
	}

	/**
	 * 检查月度目标是否已存在
	 *
	 * @param targetYear  目标年份
	 * @param targetMonth 目标月份
	 * @return 是否存在
	 */
	default boolean existsMonthlyTarget(Integer targetYear, Integer targetMonth) {
		LambdaQueryWrapperX<SalesTarget> wrapper = WrappersX.lambdaQueryX(SalesTarget.class)
				.eq(SalesTarget::getTargetYear, targetYear)
				.eq(SalesTarget::getTargetType, TargetType.MONTHLY)
				.eq(SalesTarget::getTargetMonth, targetMonth);
		return this.selectCount(wrapper) > 0;
	}

	/**
	 * 根据年份查询所有月度目标
	 *
	 * @param targetYear 目标年份
	 * @return 月度目标列表
	 */
	default List<SalesTarget> selectMonthlyTargetsByYear(Integer targetYear) {
		LambdaQueryWrapperX<SalesTarget> wrapper = WrappersX.lambdaQueryX(SalesTarget.class)
				.eq(SalesTarget::getTargetYear, targetYear)
				.eq(SalesTarget::getTargetType, TargetType.MONTHLY)
				.orderByAsc(SalesTarget::getTargetMonth);
		return this.selectList(wrapper);
	}

	/**
	 * 根据年份查询年度目标
	 *
	 * @param targetYear 目标年份
	 * @return 年度目标
	 */
	default SalesTarget selectYearlyTargetByYear(Integer targetYear) {
		LambdaQueryWrapperX<SalesTarget> wrapper = WrappersX.lambdaQueryX(SalesTarget.class)
				.eq(SalesTarget::getTargetYear, targetYear)
				.eq(SalesTarget::getTargetType, TargetType.YEARLY);
		return this.selectOne(wrapper);
	}

}