package com.erp.admin.wms.mapper;

import java.util.List;

import com.erp.admin.wms.model.entity.WmsZone;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * 品质分区 Mapper。WMS 表不在租户白名单，自动不注入 tenant_id。
 *
 * @author erp
 */
public interface WmsZoneMapper extends ExtendMapper<WmsZone> {

	/**
	 * 查询仓库下的分区列表。
	 * @param warehouseId 仓库ID
	 * @return 分区列表
	 */
	default List<WmsZone> listByWarehouse(Long warehouseId) {
		return this.selectList(WrappersX.lambdaQueryX(WmsZone.class)
			.eq(WmsZone::getWarehouseId, warehouseId)
			.orderByAsc(WmsZone::getId));
	}

	/**
	 * 查询仓库的默认标准(STANDARD)分区ID。
	 * @param warehouseId 仓库ID
	 * @return 分区ID，不存在返回 null
	 */
	default Long findDefaultStandardZoneId(Long warehouseId) {
		LambdaQueryWrapperX<WmsZone> wrapper = WrappersX.lambdaQueryX(WmsZone.class)
			.select(WmsZone::getId)
			.eq(WmsZone::getWarehouseId, warehouseId)
			.eq(WmsZone::getZoneType, "STANDARD")
			.orderByAsc(WmsZone::getId)
			.last("LIMIT 1");
		WmsZone zone = this.selectOne(wrapper);
		return zone == null ? null : zone.getId();
	}

	/**
	 * 仓库下是否已有任意分区。
	 * @param warehouseId 仓库ID
	 * @return 有则 true
	 */
	default boolean existsByWarehouse(Long warehouseId) {
		return this.selectCount(WrappersX.lambdaQueryX(WmsZone.class).eq(WmsZone::getWarehouseId, warehouseId)) > 0;
	}

}
