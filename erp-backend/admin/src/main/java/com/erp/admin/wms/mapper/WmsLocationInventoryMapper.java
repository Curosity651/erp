package com.erp.admin.wms.mapper;

import java.util.List;

import com.erp.admin.wms.model.dto.LocationInventoryKey;
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

	WmsLocationInventory selectByKeyForUpdate(@Param("key") LocationInventoryKey key);

	List<WmsLocationInventory> selectOwnedAvailableForUpdate(@Param("tenantId") Long tenantId,
			@Param("wmsTenantId") Long wmsTenantId, @Param("warehouseId") Long warehouseId,
			@Param("erpTenantId") Long erpTenantId, @Param("skuCode") String skuCode,
			@Param("quality") String quality);

	List<String> listOccupiedLocationCodes(@Param("warehouseId") Long warehouseId);

	List<Long> listBlockingWmsTenantIdsByRack(@Param("warehouseId") Long warehouseId,
			@Param("rackNo") String rackNo);

	Long sumQuantityByProviderAndWarehouse(@Param("wmsTenantId") Long wmsTenantId,
			@Param("warehouseId") Long warehouseId);

	Long sumReservedByProviderAndWarehouse(@Param("wmsTenantId") Long wmsTenantId,
			@Param("warehouseId") Long warehouseId);

	int increaseQuantity(@Param("id") Long id, @Param("quantity") int quantity,
			@Param("version") int version);

	int decreaseAvailableQuantity(@Param("id") Long id, @Param("quantity") int quantity,
			@Param("version") int version);

	int reserveQuantity(@Param("id") Long id, @Param("quantity") int quantity,
			@Param("version") int version);

	int releaseQuantity(@Param("id") Long id, @Param("quantity") int quantity,
			@Param("version") int version);

	int shipQuantity(@Param("id") Long id, @Param("quantity") int quantity,
			@Param("version") int version);

}
