package com.erp.admin.wms.service;

import java.util.List;

import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.model.entity.WmsLocation;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 库位服务（C1）。
 *
 * @author erp
 */
@Service
public class WmsLocationService extends ExtendServiceImpl<WmsLocationMapper, WmsLocation> {

	public List<WmsLocation> listByWarehouse(Long warehouseId) {
		return baseMapper.listByWarehouse(warehouseId);
	}

	public List<WmsLocation> listPhysicalByWarehouse(Long warehouseId) {
		return baseMapper.listPhysicalByWarehouse(warehouseId);
	}

	public long countByWarehouse(Long warehouseId) {
		return baseMapper.countByWarehouse(warehouseId);
	}

	public int deleteByWarehouse(Long warehouseId) {
		return baseMapper.deleteByWarehouse(warehouseId);
	}

	public int deletePhysicalByWarehouse(Long warehouseId) {
		return baseMapper.deletePhysicalByWarehouse(warehouseId);
	}

	public long countPhysicalByWarehouse(Long warehouseId) {
		return baseMapper.countPhysicalByWarehouse(warehouseId);
	}

}
