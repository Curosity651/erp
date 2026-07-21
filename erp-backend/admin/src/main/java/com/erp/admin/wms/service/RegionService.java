package com.erp.admin.wms.service;

import com.erp.admin.wms.converter.RegionConverter;
import com.erp.admin.wms.mapper.RegionMapper;
import com.erp.admin.wms.model.dto.RegionDTO;
import com.erp.admin.wms.model.entity.Region;
import com.erp.admin.wms.model.qo.RegionQO;
import com.erp.admin.wms.model.vo.RegionDisplayVO;
import com.erp.admin.wms.model.vo.RegionOptionVO;
import com.erp.admin.wms.model.vo.RegionPageVO;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RegionService extends ExtendServiceImpl<RegionMapper, Region> {

	/**
	 * 分页查询区域列表（纯分页，跨域数据填充由 Facade 层完成）
	 */
	public PageResult<RegionPageVO> queryPage(PageParam pageParam, RegionQO qo) {
		return baseMapper.queryPage(pageParam, qo);
	}

	/**
	 * 获取区域下拉选项（仅启用的区域）
	 */
	public List<RegionOptionVO> getRegionOptions() {
		return baseMapper.selectRegionOptions();
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean createRegion(RegionDTO dto) {
		this.checkCodeUnique(dto.getRegionCode(), null);
		Region region = RegionConverter.INSTANCE.dtoToEntity(dto);
		return this.save(region);
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean updateRegion(RegionDTO dto) {
		Assert.notNull(dto.getId(), "区域ID不能为空");
		Region existing = this.getById(dto.getId());
		Assert.notNull(existing, "区域不存在");
		// 区域编码不可修改
		Assert.isTrue(existing.getRegionCode().equals(dto.getRegionCode()), "区域编码不可修改");
		Region region = RegionConverter.INSTANCE.dtoToEntity(dto);
		return this.updateById(region);
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean deleteRegion(Long id) {
		return this.removeById(id);
	}

	private void checkCodeUnique(String regionCode, Long excludeId) {
		Region existing = baseMapper.selectByCode(regionCode);
		if (existing != null && !existing.getId().equals(excludeId)) {
			throw new IllegalArgumentException("区域编码已存在: " + regionCode);
		}
	}

	/**
	 * 根据区域ID集合获取区域名称映射
	 */
	public Map<Long, String> getNameMapByIds(Collection<Long> regionIds) {
		if (regionIds == null || regionIds.isEmpty()) {
			return Collections.emptyMap();
		}
		return this.listByIds(regionIds).stream()
				.collect(Collectors.toMap(Region::getId, Region::getRegionName, (a, b) -> a));
	}

	/**
	 * 查询所有启用区域
	 */
	public List<Region> listEnabled() {
		return baseMapper.selectEnabledList();
	}

	/**
	 * 泛型填充方法 - 为 VO 列表填充区域展示信息
	 *
	 * @param voList            VO 列表
	 * @param regionIdExtractor 区域ID提取函数
	 * @param displaySetter     区域展示信息设置函数
	 */
	public <T> void enrichRegionDisplay(
			List<T> voList,
			Function<T, Long> regionIdExtractor,
			BiConsumer<T, RegionDisplayVO> displaySetter) {
		if (voList == null || voList.isEmpty()) {
			return;
		}

		// 收集所有区域ID（过滤掉 null 和 0）
		Set<Long> regionIds = voList.stream()
				.map(regionIdExtractor)
				.filter(id -> id != null && id > 0)
				.collect(Collectors.toSet());

		if (regionIds.isEmpty()) {
			return;
		}

		// 批量查询区域信息
		Map<Long, Region> regionMap = this.listByIds(regionIds).stream()
				.collect(Collectors.toMap(Region::getId, r -> r, (a, b) -> a));

		// 填充展示信息
		for (T vo : voList) {
			Long regionId = regionIdExtractor.apply(vo);
			if (regionId != null && regionId > 0) {
				Region region = regionMap.get(regionId);
				if (region != null) {
					RegionDisplayVO display = new RegionDisplayVO();
					display.setRegionId(region.getId());
					display.setRegionCode(region.getRegionCode());
					display.setRegionName(region.getRegionName());
					displaySetter.accept(vo, display);
				}
			}
		}
	}

}
