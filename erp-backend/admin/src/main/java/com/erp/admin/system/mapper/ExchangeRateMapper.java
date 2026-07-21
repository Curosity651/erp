package com.erp.admin.system.mapper;

import java.time.LocalDate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.system.model.entity.ExchangeRate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

/**
 * 汇率Mapper
 *
 * @author system
 */
@Mapper
public interface ExchangeRateMapper extends ExtendMapper<ExchangeRate> {

	/**
	 * 查询指定日期和货币对是否已存在
	 *
	 * @param baseCurrencyCode 基准币种
	 * @param targetCurrencyCode 目标币种
	 * @param rateDate 汇率日期
	 * @return 存在返回1,不存在返回0
	 */
	default int existsByDateAndCurrencyPair(@Param("baseCurrencyCode") String baseCurrencyCode,
			@Param("targetCurrencyCode") String targetCurrencyCode, @Param("rateDate") LocalDate rateDate) {
		LambdaQueryWrapper<ExchangeRate> wrapper = Wrappers.lambdaQuery(ExchangeRate.class)
			.eq(ExchangeRate::getBaseCurrencyCode, baseCurrencyCode)
			.eq(ExchangeRate::getTargetCurrencyCode, targetCurrencyCode)
			.eq(ExchangeRate::getRateDate, rateDate);
		return Math.toIntExact(selectCount(wrapper));
	}

	/**
	 * 获取最新的汇率数据
	 *
	 * @param baseCurrencyCode 基准币种
	 * @param targetCurrencyCode 目标币种
	 * @return 最新汇率记录
	 */
	default ExchangeRate getLatestRate(@Param("baseCurrencyCode") String baseCurrencyCode,
			@Param("targetCurrencyCode") String targetCurrencyCode) {
		LambdaQueryWrapper<ExchangeRate> wrapper = Wrappers.lambdaQuery(ExchangeRate.class)
			.eq(ExchangeRate::getBaseCurrencyCode, baseCurrencyCode)
			.eq(ExchangeRate::getTargetCurrencyCode, targetCurrencyCode)
			.orderByDesc(ExchangeRate::getRateDate, ExchangeRate::getId)
			.last("LIMIT 1");
		return selectOne(wrapper);
	}

	/**
	 * 获取指定日期的汇率数据
	 *
	 * @param baseCurrencyCode   基准币种
	 * @param targetCurrencyCode 目标币种
	 * @param rateDate           汇率日期
	 * @return 指定日期的汇率记录
	 */
	default ExchangeRate getRateByDate(@Param("baseCurrencyCode") String baseCurrencyCode,
			@Param("targetCurrencyCode") String targetCurrencyCode, @Param("rateDate") LocalDate rateDate) {
		LambdaQueryWrapper<ExchangeRate> wrapper = Wrappers.lambdaQuery(ExchangeRate.class)
			.eq(ExchangeRate::getBaseCurrencyCode, baseCurrencyCode)
			.eq(ExchangeRate::getTargetCurrencyCode, targetCurrencyCode)
			.eq(ExchangeRate::getRateDate, rateDate);
		return selectOne(wrapper);
	}

}
