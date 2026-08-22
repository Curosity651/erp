package com.erp.admin.wms.mapper;

import java.util.List;

import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.vo.WarehouseLocationSummaryVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * 库位 Mapper。WMS 表不在租户白名单，自动不注入 tenant_id。
 *
 * @author erp
 */
public interface WmsLocationMapper extends ExtendMapper<WmsLocation> {

	@Select("<script>"
			+ "SELECT * FROM wms_location "
			+ "WHERE warehouse_id = #{warehouseId} AND is_virtual = 0 AND deleted = 0 "
			+ "AND id IN "
			+ "<foreach collection='locationIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
			+ "ORDER BY id FOR UPDATE"
			+ "</script>")
	List<WmsLocation> selectPhysicalByIdsForUpdate(@Param("warehouseId") Long warehouseId,
			@Param("locationIds") List<Long> locationIds);

	@Update("<script>"
			+ "UPDATE wms_location SET zone_id = #{zoneId} "
			+ "WHERE warehouse_id = #{warehouseId} AND is_virtual = 0 AND deleted = 0 "
			+ "AND id IN "
			+ "<foreach collection='locationIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
			+ "</script>")
	int updateZoneBatch(@Param("warehouseId") Long warehouseId, @Param("locationIds") List<Long> locationIds,
			@Param("zoneId") Long zoneId);

	@Select("SELECT * FROM wms_location WHERE id = #{id} AND deleted = 0 FOR UPDATE")
	WmsLocation selectByIdForUpdate(@Param("id") Long id);

	@Select("SELECT * FROM wms_location WHERE id = #{id} AND deleted = 0 FOR UPDATE")
	WmsLocation selectLogicalByIdForUpdate(@Param("id") Long id);

	@Select("SELECT COUNT(*) FROM wms_location WHERE warehouse_id = #{warehouseId} AND location_code = #{locationCode}")
	long countIncludingDeletedByCode(@Param("warehouseId") Long warehouseId,
			@Param("locationCode") String locationCode);

	@Select("SELECT COUNT(*) FROM wms_location WHERE warehouse_id = #{warehouseId} "
			+ "AND rack_no = #{rackNo} AND column_no = #{sequenceNo}")
	long countIncludingDeletedBySequence(@Param("warehouseId") Long warehouseId,
			@Param("rackNo") String rackNo, @Param("sequenceNo") Integer sequenceNo);

	/**
	 * 查询仓库下的库位列表（按排/列）。
	 * @param warehouseId 仓库ID
	 * @return 库位列表
	 */
	default List<WmsLocation> listByWarehouse(Long warehouseId) {
		return this.selectList(WrappersX.lambdaQueryX(WmsLocation.class)
			.eq(WmsLocation::getWarehouseId, warehouseId)
			.orderByAsc(WmsLocation::getRackNo)
			.orderByAsc(WmsLocation::getColumnNo));
	}

	default List<WmsLocation> listPhysicalByWarehouse(Long warehouseId) {
		return this.selectList(WrappersX.lambdaQueryX(WmsLocation.class)
			.eq(WmsLocation::getWarehouseId, warehouseId)
			.eq(WmsLocation::getIsVirtual, 0)
			.orderByAsc(WmsLocation::getRackNo)
			.orderByAsc(WmsLocation::getColumnNo));
	}

	default List<WmsLocation> listAssignableByWarehouse(Long warehouseId) {
		return this.selectList(WrappersX.lambdaQueryX(WmsLocation.class)
			.eq(WmsLocation::getWarehouseId, warehouseId)
			.eq(WmsLocation::getIsVirtual, 0)
			.and(query -> query.isNull(WmsLocation::getPublicShared)
				.or()
				.eq(WmsLocation::getPublicShared, 0))
			.orderByAsc(WmsLocation::getRackNo)
			.orderByAsc(WmsLocation::getColumnNo));
	}

	@Select("<script>"
			+ "SELECT warehouse_id AS warehouseId, COUNT(*) AS actualLocationCount, "
			+ "COUNT(DISTINCT rack_no) AS actualRackCount, "
			+ "COUNT(DISTINCT CASE WHEN is_virtual = 0 AND COALESCE(public_shared, 0) = 0 "
			+ "THEN rack_no END) AS assignableRackCount "
			+ "FROM wms_location WHERE deleted = 0 AND warehouse_id IN "
			+ "<foreach collection='warehouseIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
			+ "GROUP BY warehouse_id"
			+ "</script>")
	List<WarehouseLocationSummaryVO> summarizeByWarehouseIds(@Param("warehouseIds") List<Long> warehouseIds);

	/**
	 * 删除某仓库全部库位（重新生成前清旧）——<b>物理删除</b>。
	 * <p>WmsLocation 带 {@code @TableLogic}，若走常规逻辑删除只会把旧行标 deleted=1、仍占用唯一索引
	 * {@code uk_wh_loc(warehouse_id, location_code)}，重建插入同 location_code 时必撞 DuplicateKey 而回滚。
	 * 库位是整体替换的基础设施、无软删审计诉求，且删前已有"该仓无库存批次"守卫，故用原生 DELETE 真正清空、释放编码。
	 * @param warehouseId 仓库ID
	 * @return 删除行数
	 */
	@Delete("DELETE FROM wms_location WHERE warehouse_id = #{warehouseId}")
	int deleteByWarehouse(@Param("warehouseId") Long warehouseId);

	@Delete("DELETE FROM wms_location WHERE warehouse_id = #{warehouseId} AND is_virtual = 0")
	int deletePhysicalByWarehouse(@Param("warehouseId") Long warehouseId);

	@Delete("DELETE FROM wms_location "
			+ "WHERE id = #{id} AND warehouse_id = #{warehouseId} AND is_virtual = 1")
	int deleteVirtualById(@Param("id") Long id, @Param("warehouseId") Long warehouseId);

	/**
	 * 统计仓库下库位数。
	 * @param warehouseId 仓库ID
	 * @return 库位数
	 */
	default long countByWarehouse(Long warehouseId) {
		return this.selectCount(WrappersX.lambdaQueryX(WmsLocation.class).eq(WmsLocation::getWarehouseId, warehouseId));
	}

	default long countPhysicalByWarehouse(Long warehouseId) {
		return this.selectCount(WrappersX.lambdaQueryX(WmsLocation.class)
			.eq(WmsLocation::getWarehouseId, warehouseId)
			.eq(WmsLocation::getIsVirtual, 0));
	}

}
