package com.erp.admin.wms.mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.wms.converter.LogisticsProviderConverter;
import com.erp.admin.wms.model.entity.LogisticsProvider;
import com.erp.admin.wms.model.qo.LogisticsProviderQO;
import com.erp.admin.wms.model.vo.LogisticsProviderOptionVO;
import com.erp.admin.wms.model.vo.LogisticsProviderPageVO;
import org.ballcat.common.core.constant.enums.BooleanEnum;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * 物流商 Mapper
 *
 * @author erp
 */
public interface LogisticsProviderMapper extends ExtendMapper<LogisticsProvider> {

	/**
	 * 分页查询
	 *
	 * @param pageParam 分页参数
	 * @param qo        查询参数
	 * @return PageResult<LogisticsProviderPageVO> VO分页数据
	 */
	default PageResult<LogisticsProviderPageVO> queryPage(PageParam pageParam, LogisticsProviderQO qo) {
		IPage<LogisticsProvider> page = this.prodPage(pageParam);
		LambdaQueryWrapperX<LogisticsProvider> wrapper = WrappersX.lambdaQueryX(LogisticsProvider.class)
				.likeIfPresent(LogisticsProvider::getProviderCode, qo.getProviderCode())
				.likeIfPresent(LogisticsProvider::getProviderName, qo.getProviderName())
				.eqIfPresent(LogisticsProvider::getStatus, qo.getStatus())
				.orderByDesc(LogisticsProvider::getId);
		this.selectPage(page, wrapper);
		IPage<LogisticsProviderPageVO> voPage = page.convert(LogisticsProviderConverter.INSTANCE::poToPageVo);
		return new PageResult<>(voPage.getRecords(), voPage.getTotal());
	}

	/**
	 * 获取物流商下拉选项列表
	 *
	 * @return List<LogisticsProviderOptionVO> 物流商下拉选项列表
	 */
	default List<LogisticsProviderOptionVO> selectProviderOptions() {
		LambdaQueryWrapperX<LogisticsProvider> wrapper = WrappersX.lambdaQueryX(LogisticsProvider.class)
				.select(LogisticsProvider::getId, LogisticsProvider::getProviderCode, LogisticsProvider::getProviderName)
				.eq(LogisticsProvider::getStatus, BooleanEnum.TRUE.intValue()) // 只查询启用状态的物流商
				.orderByAsc(LogisticsProvider::getProviderCode); // 按编码排序

		List<LogisticsProvider> providers = this.selectList(wrapper);

		return providers.stream()
				.map(LogisticsProviderConverter.INSTANCE::poToOptionVo)
				.collect(Collectors.toList());
	}

	/**
	 * 批量查询物流商名称映射
	 *
	 * @param providerIds 物流商ID列表
	 * @return Map<Long, String> ID -> 名称映射
	 */
	default Map<Long, String> selectProviderNameMap(Collection<Long> providerIds) {
		if (providerIds == null || providerIds.isEmpty()) {
			return Collections.emptyMap();
		}
		LambdaQueryWrapperX<LogisticsProvider> wrapper = WrappersX.lambdaQueryX(LogisticsProvider.class)
				.select(LogisticsProvider::getId, LogisticsProvider::getProviderName)
				.in(LogisticsProvider::getId, providerIds);
		return this.selectList(wrapper).stream()
				.collect(Collectors.toMap(LogisticsProvider::getId, LogisticsProvider::getProviderName));
	}

	/**
	 * 更新物流商状态
	 *
	 * @param id     物流商ID
	 * @param status 状态: 1-启用 / 0-停用
	 * @return 影响行数
	 */
	default int updateStatus(Long id, Integer status) {
		LambdaUpdateWrapper<LogisticsProvider> updateWrapper = Wrappers.lambdaUpdate(LogisticsProvider.class)
				.eq(LogisticsProvider::getId, id)
				.set(LogisticsProvider::getStatus, status);
		return this.update(null, updateWrapper);
	}

	/**
	 * 根据物流商编码查询数量
	 *
	 * @param providerCode 物流商编码
	 * @return 数量
	 */
	default long selectCountByProviderCode(String providerCode) {
		LambdaQueryWrapper<LogisticsProvider> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(LogisticsProvider::getProviderCode, providerCode);
		return this.selectCount(wrapper);
	}
}
