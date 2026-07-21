package com.erp.admin.wms.mapper;

import java.util.List;

import com.erp.admin.wms.model.entity.WmsRackAssignment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * 货架分配 Mapper。平台级，无租户白名单注入。
 *
 * @author erp
 */
public interface WmsRackAssignmentMapper extends ExtendMapper<WmsRackAssignment> {

	/**
	 * 列出某仓库的全部分配记录（含历史）。
	 * @param warehouseId 仓库ID
	 * @return 分配列表
	 */
	default List<WmsRackAssignment> listByWarehouse(Long warehouseId) {
		return this.selectList(WrappersX.lambdaQueryX(WmsRackAssignment.class)
			.eq(WmsRackAssignment::getWarehouseId, warehouseId)
			.orderByAsc(WmsRackAssignment::getRackNo)
			.orderByDesc(WmsRackAssignment::getEffectiveFrom));
	}

	/**
	 * 列出某仓库某排的全部分配记录（用于重叠校验）。
	 * @param warehouseId 仓库ID
	 * @param rackNo 排号
	 * @return 分配列表
	 */
	default List<WmsRackAssignment> listByWarehouseAndRack(Long warehouseId, String rackNo) {
		return this.selectList(WrappersX.lambdaQueryX(WmsRackAssignment.class)
			.eq(WmsRackAssignment::getWarehouseId, warehouseId)
			.eq(WmsRackAssignment::getRackNo, rackNo));
	}

	/**
	 * 悲观锁读取某仓某排的全部分配（供 assign 重叠校验用，避免并发下两个请求都通过检查而双重分配）。
	 * wms_rack_assignment 不在任何租户/数据权限拦截表内，故返回全部服务商的分配，重叠检查对所有租户生效。
	 * 须在同一事务内调用；FOR UPDATE 锁住命中行，令同仓同排的并发 assign 串行化。
	 * @param warehouseId 仓库ID
	 * @param rackNo 排号
	 * @return 分配列表（已加行锁）
	 */
	@Select("SELECT * FROM wms_rack_assignment WHERE warehouse_id = #{warehouseId} AND rack_no = #{rackNo} FOR UPDATE")
	List<WmsRackAssignment> listByWarehouseAndRackForUpdate(@Param("warehouseId") Long warehouseId,
			@Param("rackNo") String rackNo);

	/**
	 * 列出某 WMS 服务商被分配了货架的仓库 id（去重）。用于 OWN 仓可见性收窄：
	 * 服务商看自己有货架分配的仓；货主看其父服务商有货架分配的仓（均传 WmsTenantContext）。
	 * @param wmsTenantId 服务商租户 id
	 * @return 仓库 id 列表
	 */
	default List<Long> listWarehouseIdsByWmsTenant(Long wmsTenantId) {
		if (wmsTenantId == null) {
			return java.util.Collections.emptyList();
		}
		return this.selectList(WrappersX.lambdaQueryX(WmsRackAssignment.class)
			.select(WmsRackAssignment::getWarehouseId)
			.eq(WmsRackAssignment::getWmsTenantId, wmsTenantId))
			.stream().map(WmsRackAssignment::getWarehouseId).distinct().collect(java.util.stream.Collectors.toList());
	}

}
