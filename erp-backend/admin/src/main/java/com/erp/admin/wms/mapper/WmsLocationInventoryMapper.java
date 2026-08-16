package com.erp.admin.wms.mapper;

import java.util.List;

import com.erp.admin.wms.model.entity.WmsLocationInventory;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

/**
 * Mapper for the batch-free logical location inventory.
 */
public interface WmsLocationInventoryMapper extends ExtendMapper<WmsLocationInventory> {

	WmsLocationInventory selectForUpdate(@Param("id") Long id);

	List<WmsLocationInventory> selectAvailableForUpdate(@Param("warehouseId") Long warehouseId,
			@Param("erpTenantId") Long erpTenantId, @Param("skuCode") String skuCode,
			@Param("quality") String quality);

}
