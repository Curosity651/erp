package com.erp.admin.wms.service;

import com.erp.admin.wms.mapper.PlatformRegionMappingMapper;
import com.erp.admin.wms.model.entity.PlatformRegionMapping;
import com.erp.admin.wms.model.vo.PlatformRegionMappingVO;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlatformRegionMappingService extends ExtendServiceImpl<PlatformRegionMappingMapper, PlatformRegionMapping> {

	/**
	 * 查询所有平台映射
	 */
	public List<PlatformRegionMappingVO> listAll() {
		return baseMapper.selectAll();
	}

	/**
	 * 查询指定区域关联的平台标识列表
	 */
	public List<String> getPlatformsByRegionId(Long regionId) {
		return baseMapper.selectPlatformsByRegionId(regionId);
	}

	/**
	 * 批量查询多个区域的平台列表（避免 N+1 查询）
	 * @return key=regionId, value=平台标识列表
	 */
	public Map<Long, List<String>> getPlatformsGroupByRegionId(List<Long> regionIds) {
		if (regionIds == null || regionIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<PlatformRegionMapping> mappings = baseMapper.selectByRegionIds(regionIds);
		return mappings.stream().collect(Collectors.groupingBy(
				PlatformRegionMapping::getRegionId,
				Collectors.mapping(PlatformRegionMapping::getPlatform, Collectors.toList())
		));
	}

	/**
	 * 判断指定区域是否存在平台映射
	 */
	public boolean existsByRegionId(Long regionId) {
		return baseMapper.existsByRegionId(regionId);
	}

	/**
	 * 根据平台编码获取关联的区域 ID
	 * @param platform 平台编码 (如 ozon)
	 * @return 区域 ID，未找到映射返回 null
	 */
	public Long getRegionIdByPlatform(String platform) {
		PlatformRegionMapping mapping = baseMapper.selectByPlatform(platform);
		return mapping != null ? mapping.getRegionId() : null;
	}

	/**
	 * 保存平台映射（新增或更新或删除）
	 * @param platform 平台标识
	 * @param regionId 区域ID，为null时删除映射
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean saveMapping(String platform, Long regionId) {
		PlatformRegionMapping existing = baseMapper.selectByPlatform(platform);
		if (regionId == null) {
			// 清空映射
			if (existing != null) {
				return this.removeById(existing.getId());
			}
			return true;
		}
		if (existing != null) {
			// 更新映射
			if (existing.getRegionId().equals(regionId)) {
				return true; // 无变化
			}
			existing.setRegionId(regionId);
			return this.updateById(existing);
		}
		// 新增映射
		PlatformRegionMapping mapping = new PlatformRegionMapping();
		mapping.setPlatform(platform);
		mapping.setRegionId(regionId);
		return this.save(mapping);
	}

}
