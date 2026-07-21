package com.erp.admin.wms.facade;

import com.erp.admin.wms.model.dto.RegionDTO;
import com.erp.admin.wms.model.qo.RegionQO;
import com.erp.admin.wms.model.vo.PlatformRegionMappingVO;
import com.erp.admin.wms.model.vo.RegionOptionVO;
import com.erp.admin.wms.model.vo.RegionPageVO;
import com.erp.admin.wms.service.PlatformRegionMappingService;
import com.erp.admin.wms.service.RegionService;
import com.erp.admin.wms.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 区域管理编排层
 * <p>
 * 负责跨域编排：Region + Warehouse + PlatformRegionMapping，
 * 使三个 Service 之间保持零耦合。
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegionManageFacade {

	private final RegionService regionService;

	private final WarehouseService warehouseService;

	private final PlatformRegionMappingService platformRegionMappingService;

	// ========== 区域 CRUD ==========

	/**
	 * 分页查询区域列表，附带关联平台和仓库数量
	 */
	public PageResult<RegionPageVO> queryPage(PageParam pageParam, RegionQO qo) {
		PageResult<RegionPageVO> result = regionService.queryPage(pageParam, qo);
		List<RegionPageVO> records = result.getRecords();
		if (records.isEmpty()) {
			return result;
		}
		List<Long> regionIds = records.stream()
				.map(RegionPageVO::getId)
				.collect(Collectors.toList());
		// 跨域聚合：平台映射 + 仓库数量
		Map<Long, List<String>> platformMap = platformRegionMappingService
				.getPlatformsGroupByRegionId(regionIds);
		Map<Long, Integer> warehouseCountMap = warehouseService
				.countByRegionIds(regionIds);
		for (RegionPageVO vo : records) {
			vo.setPlatforms(platformMap.getOrDefault(vo.getId(), Collections.emptyList()));
			vo.setWarehouseCount(warehouseCountMap.getOrDefault(vo.getId(), 0));
		}
		return result;
	}

	/**
	 * 区域下拉选项
	 */
	public List<RegionOptionVO> getRegionOptions() {
		return regionService.getRegionOptions();
	}

	/**
	 * 新增区域
	 */
	public boolean createRegion(RegionDTO dto) {
		return regionService.createRegion(dto);
	}

	/**
	 * 编辑区域
	 */
	public boolean updateRegion(RegionDTO dto) {
		return regionService.updateRegion(dto);
	}

	/**
	 * 删除区域（跨域校验：仓库引用 + 平台映射引用）
	 */
	public boolean deleteRegion(Long id) {
		Assert.isTrue(warehouseService.countByRegionId(id) == 0,
				"该区域下存在仓库，无法删除");
		Assert.isTrue(!platformRegionMappingService.existsByRegionId(id),
				"该区域存在平台映射，无法删除");
		return regionService.removeById(id);
	}

	// ========== 平台映射（透传） ==========

	public List<PlatformRegionMappingVO> getAllPlatformMappings() {
		return platformRegionMappingService.listAll();
	}

	public boolean savePlatformMapping(String platform, Long regionId) {
		return platformRegionMappingService.saveMapping(platform, regionId);
	}

}
