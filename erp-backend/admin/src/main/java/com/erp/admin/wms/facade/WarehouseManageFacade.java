package com.erp.admin.wms.facade;

import com.erp.admin.wms.model.dto.WarehouseDTO;
import com.erp.admin.wms.model.entity.Region;
import com.erp.admin.wms.model.enums.WarehouseTypeEnum;
import com.erp.admin.wms.model.qo.WarehouseQO;
import com.erp.admin.wms.model.vo.WarehousePageVO;
import com.erp.admin.wms.service.RegionService;
import com.erp.admin.wms.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 仓库管理编排层
 * <p>
 * 负责跨域编排：Warehouse + Region，
 * 处理 regionId 校验、regionName 填充等跨域逻辑。
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseManageFacade {

	private final WarehouseService warehouseService;

	private final RegionService regionService;

	/**
	 * 分页查询仓库（附带区域名称填充）
	 */
	public PageResult<WarehousePageVO> queryPage(PageParam pageParam, WarehouseQO qo) {
		PageResult<WarehousePageVO> result = warehouseService.queryPage(pageParam, qo);
		enrichRegionName(result.getRecords());
		return result;
	}

	/**
	 * 新增仓库（含跨域 regionId 校验）
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean createWarehouse(WarehouseDTO dto) {
		validateRegionId(dto);
		return warehouseService.createWarehouse(dto);
	}

	/**
	 * 修改仓库（含跨域 regionId 校验）
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean updateWarehouse(WarehouseDTO dto) {
		validateRegionId(dto);
		return warehouseService.updateWarehouse(dto);
	}

	// ========== 跨域私有方法 ==========

	/**
	 * 校验仓库的 regionId 有效性（非 FBO 仓库必须指定区域）
	 */
	private void validateRegionId(WarehouseDTO dto) {
		if (!WarehouseTypeEnum.FBO.getCode().equals(dto.getWarehouseType())) {
			Assert.notNull(dto.getRegionId(), "非FBO仓库必须指定所属区域");
		}
		if (dto.getRegionId() != null) {
			Region region = regionService.getById(dto.getRegionId());
			Assert.notNull(region, "所选区域不存在");
		}
	}

	/**
	 * 填充分页结果中的区域名称
	 */
	private void enrichRegionName(List<WarehousePageVO> records) {
		if (records == null || records.isEmpty()) {
			return;
		}
		Set<Long> regionIds = records.stream()
				.map(WarehousePageVO::getRegionId)
				.filter(id -> id != null && id > 0)
				.collect(Collectors.toSet());
		if (regionIds.isEmpty()) {
			return;
		}
		List<Region> regions = regionService.listByIds(regionIds);
		Map<Long, String> regionNameMap = regions.stream()
				.collect(Collectors.toMap(Region::getId, Region::getRegionName));
		for (WarehousePageVO vo : records) {
			if (vo.getRegionId() != null && vo.getRegionId() > 0) {
				vo.setRegionName(regionNameMap.get(vo.getRegionId()));
			}
		}
	}

}
