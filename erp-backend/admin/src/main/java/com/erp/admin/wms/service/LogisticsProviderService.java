package com.erp.admin.wms.service;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.util.CollectionUtils;
import com.erp.admin.wms.converter.LogisticsProviderConverter;
import com.erp.admin.wms.mapper.LogisticsProviderMapper;
import com.erp.admin.wms.model.dto.LogisticsProviderDTO;
import com.erp.admin.wms.model.entity.LogisticsProvider;
import com.erp.admin.wms.model.qo.LogisticsProviderQO;
import com.erp.admin.wms.model.vo.LogisticsProviderOptionVO;
import com.erp.admin.wms.model.vo.LogisticsProviderPageVO;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * 物流商管理服务
 *
 * @author erp
 */
@Service
public class LogisticsProviderService extends ExtendServiceImpl<LogisticsProviderMapper, LogisticsProvider> {

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param qo 查询参数对象
	 * @return PageResult<LogisticsProviderPageVO> 分页数据
	 */
	public PageResult<LogisticsProviderPageVO> queryPage(PageParam pageParam, LogisticsProviderQO qo) {
		return baseMapper.queryPage(pageParam, qo);
	}

	/**
	 * 获取物流商下拉选项列表
	 * @return List<LogisticsProviderOptionVO> 物流商下拉选项列表
	 */
	public List<LogisticsProviderOptionVO> getProviderOptions() {
		return baseMapper.selectProviderOptions();
	}

	/**
	 * 获取物流商详情
	 * @param id 物流商ID
	 * @return LogisticsProviderPageVO 物流商详情
	 */
	public LogisticsProviderPageVO getDetail(Long id) {
		LogisticsProvider provider = this.getById(id);
		return LogisticsProviderConverter.INSTANCE.poToPageVo(provider);
	}

	/**
	 * 创建物流商
	 * @param dto 物流商数据传输对象
	 * @return boolean 是否创建成功
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean createProvider(LogisticsProviderDTO dto) {
		Assert.isNull(dto.getId(), "物流商ID必须为空");
		// 校验物流商编码唯一性
		this.checkProviderCodeUnique(dto.getProviderCode());
		LogisticsProvider logisticsProvider = LogisticsProviderConverter.INSTANCE.dtoToEntity(dto);
		return this.save(logisticsProvider);
	}

	/**
	 * 更新物流商
	 * @param dto 物流商数据传输对象
	 * @return boolean 是否更新成功
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean updateProvider(LogisticsProviderDTO dto) {
		Assert.notNull(dto.getId(), "物流商ID不能为空");

		// 编辑时不允许修改编码，所以不需要校验编码唯一性
		LogisticsProvider logisticsProvider = LogisticsProviderConverter.INSTANCE.dtoToEntity(dto);
		logisticsProvider.setProviderCode(null);

		return this.updateById(logisticsProvider);
	}

	/**
	 * 校验物流商编码唯一性
	 * @param providerCode 物流商编码
	 */
	private void checkProviderCodeUnique(String providerCode) {
		long count = baseMapper.selectCountByProviderCode(providerCode);
		Assert.isTrue(count == 0, "物流商编码已存在");
	}

	/**
	 * 更新物流商状态
	 * @param id 物流商ID
	 * @param status 状态: 1-启用 / 0-停用
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateStatus(Long id, Integer status) {
		int rows = baseMapper.updateStatus(id, status);
		if (rows == 0) {
			throw new IllegalArgumentException("物流商不存在");
		}
	}

	/**
	 * 根据ID获取物流商名称
	 * @param providerId 物流商ID
	 * @return 物流商名称
	 */
	public String getNameById(Long providerId) {
		if (providerId == null) {
			return null;
		}
		LogisticsProvider provider = this.getById(providerId);
		return provider != null ? provider.getProviderName() : null;
	}

	/**
	 * 批量根据ID获取物流商名称映射
	 * @param providerIds 物流商ID集合
	 * @return Map<providerId, providerName>
	 */
	public Map<Long, String> getNameMapByIds(Collection<Long> providerIds) {
		if (CollectionUtils.isEmpty(providerIds)) {
			return Collections.emptyMap();
		}
		List<LogisticsProvider> providers = this.listByIds(providerIds);
		return providers.stream()
				.collect(Collectors.toMap(LogisticsProvider::getId, LogisticsProvider::getProviderName, (v1, v2) -> v1));
	}


	/**
	 * 批量填充物流商名称
	 * @param records 记录列表
	 * @param providerIdGetter 物流商ID获取函数
	 * @param providerNameSetter 物流商名称设置函数
	 * @param <T> 记录类型
	 */
	public <T> void enrichProviderName(
			List<T> records,
			Function<T, Long> providerIdGetter,
			BiConsumer<T, String> providerNameSetter) {

		if (records == null || records.isEmpty()) {
			return;
		}

		Set<Long> providerIds = records.stream()
				.map(providerIdGetter)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (providerIds.isEmpty()) {
			return;
		}

		Map<Long, String> nameMap = this.getNameMapByIds(providerIds);

		for (T record : records) {
			Long providerId = providerIdGetter.apply(record);
			if (providerId != null) {
				providerNameSetter.accept(record, nameMap.get(providerId));
			}
		}
	}

}
