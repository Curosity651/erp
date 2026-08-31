package com.erp.admin.wms.mapper;

import java.time.LocalDate;
import java.util.List;

import com.erp.admin.wms.model.vo.StorageOverviewVO;
import org.apache.ibatis.annotations.Param;

/**
 * 服务商「仓储概览」聚合 Mapper。容量按有效排分配统计，库存按 physical_inventory.wms_tenant_id
 * 和货主父服务商双重收窄；公共暂存库存只计入库存，不计入分配容量。纯只读。
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
	 * 某仓内该服务商名下库存按货主拆分（包含公共暂存库存，不返回具体库位名称）。
	 * @param wmsTenantId 当前服务商
	 * @param warehouseId 仓库
	 * @param today 当日
	 * @return 货主行列表
	 */
	List<StorageOverviewVO.OwnerRow> listOwners(@Param("wmsTenantId") Long wmsTenantId,
			@Param("warehouseId") Long warehouseId, @Param("today") LocalDate today);

}
