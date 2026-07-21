package com.erp.admin.wms.mapper;

import java.time.LocalDate;
import java.util.List;

import com.erp.admin.wms.model.vo.StorageOverviewVO;
import org.apache.ibatis.annotations.Param;

/**
 * 服务商「仓储概览」聚合 Mapper。全部按当前服务商 wms_tenant_id 限定；占用归属靠货架分配 JOIN 收窄，
 * 不依赖 physical_inventory.wms_tenant_id（该列历史数据可能为 0）。纯只读。
 *
 * @author erp
 */
public interface WmsStorageOverviewMapper {

	/**
	 * 服务商名下各仓的分配与占用汇总（仅含有 active 货架分配的仓）。
	 * @param wmsTenantId 当前服务商
	 * @param today 当日（判定分配是否有效）
	 * @return 仓库行列表
	 */
	List<StorageOverviewVO.WarehouseRow> listWarehouses(@Param("wmsTenantId") Long wmsTenantId,
			@Param("today") LocalDate today);

	/**
	 * 某仓内该服务商被分配货架的维度明细（库位数/占用/月租/到期）。
	 * @param wmsTenantId 当前服务商
	 * @param warehouseId 仓库
	 * @param today 当日
	 * @return 货架行列表
	 */
	List<StorageOverviewVO.RackRow> listRacks(@Param("wmsTenantId") Long wmsTenantId,
			@Param("warehouseId") Long warehouseId, @Param("today") LocalDate today);

	/**
	 * 某仓内该服务商货架上、按货主拆分的占用（库位数/件数/SKU 数）。
	 * @param wmsTenantId 当前服务商
	 * @param warehouseId 仓库
	 * @param today 当日
	 * @return 货主行列表
	 */
	List<StorageOverviewVO.OwnerRow> listOwners(@Param("wmsTenantId") Long wmsTenantId,
			@Param("warehouseId") Long warehouseId, @Param("today") LocalDate today);

}
