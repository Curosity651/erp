package com.erp.admin.wms.mapper;

import java.time.LocalDate;
import java.util.List;

import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.vo.LocationTransferSourceBatchVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * 批次级库存 SSOT Mapper。WMS 表不在租户白名单，自动不注入 tenant_id（按双 ID 显式过滤）。
 *
 * @author erp
 */
public interface WmsPhysicalInventoryMapper extends ExtendMapper<WmsPhysicalInventory> {

	@Select("SELECT * FROM wms_physical_inventory WHERE id = #{id} FOR UPDATE")
	WmsPhysicalInventory selectByIdForUpdate(@Param("id") Long id);

	@Select("SELECT * FROM wms_physical_inventory "
			+ "WHERE pallet_id = #{palletId} AND quantity > 0 ORDER BY id FOR UPDATE")
	List<WmsPhysicalInventory> selectByPalletIdForUpdate(@Param("palletId") Long palletId);

	@Select("<script>"
			+ "SELECT pi.id, pi.warehouse_id, pi.wms_tenant_id, pi.erp_tenant_id, "
			+ "t.tenant_name AS owner_name, pi.sku_code, pi.quantity, pi.reserved_qty, "
			+ "pi.quality, pi.inbound_date, pi.location_code, pi.zone_id, "
			+ "pi.pallet_id, p.pallet_no, p.pallet_status, p.pallet_type, "
			+ "pi.slot_id, s.slot_code "
			+ "FROM wms_physical_inventory pi "
			+ "LEFT JOIN wms_pallet p ON p.id = pi.pallet_id "
			+ "LEFT JOIN wms_location_slot s ON s.id = pi.slot_id "
			+ "LEFT JOIN sys_tenant t ON t.id = pi.erp_tenant_id "
			+ "WHERE pi.warehouse_id = #{warehouseId} AND pi.quantity &gt; 0 "
			+ "<if test='locationCode != null and locationCode != \"\"'>"
			+ "AND pi.location_code = #{locationCode} "
			+ "</if>"
			+ "<if test='erpTenantId != null'>AND pi.erp_tenant_id = #{erpTenantId} </if>"
			+ "<if test='skuKeyword != null and skuKeyword != \"\"'>"
			+ "AND (pi.sku_code LIKE CONCAT('%', #{skuKeyword}, '%') "
			+ "OR CONCAT(t.warehouse_sku_prefix, '-', pi.sku_code) LIKE CONCAT('%', #{skuKeyword}, '%')) "
			+ "</if>"
			+ "<if test='palletNo != null and palletNo != \"\"'>"
			+ "AND p.pallet_no LIKE CONCAT('%', #{palletNo}, '%') "
			+ "</if>"
			+ "ORDER BY pi.location_code, s.level_no, s.position_no, p.pallet_no, pi.sku_code, pi.id"
			+ "</script>")
	List<LocationTransferSourceBatchVO> listTransferSources(@Param("warehouseId") Long warehouseId,
			@Param("locationCode") String locationCode, @Param("erpTenantId") Long erpTenantId,
			@Param("skuKeyword") String skuKeyword, @Param("palletNo") String palletNo);

	@Select("SELECT DISTINCT pi.location_code "
			+ "FROM wms_physical_inventory pi "
			+ "JOIN wms_location l ON l.warehouse_id = pi.warehouse_id "
			+ " AND l.location_code = pi.location_code AND l.deleted = 0 "
			+ "WHERE pi.warehouse_id = #{warehouseId} AND l.is_virtual = 0 "
			+ "AND (COALESCE(pi.quantity, 0) > 0 OR COALESCE(pi.reserved_qty, 0) > 0)")
	List<String> listBlockingPhysicalLocationCodes(@Param("warehouseId") Long warehouseId);

	@Select("SELECT DISTINCT pi.wms_tenant_id "
			+ "FROM wms_physical_inventory pi "
			+ "JOIN wms_location l ON l.warehouse_id = pi.warehouse_id "
			+ " AND l.location_code = pi.location_code AND l.deleted = 0 AND l.is_virtual = 0 "
			+ "WHERE pi.warehouse_id = #{warehouseId} AND l.rack_no = #{rackNo} "
			+ "AND (COALESCE(pi.quantity, 0) > 0 OR COALESCE(pi.reserved_qty, 0) > 0)")
	List<Long> listBlockingWmsTenantIdsByRack(@Param("warehouseId") Long warehouseId,
			@Param("rackNo") String rackNo);

	default List<WmsPhysicalInventory> listByWarehouse(Long warehouseId) {
		return this.selectList(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
				.eq(WmsPhysicalInventory::getWarehouseId, warehouseId)
				.orderByAsc(WmsPhysicalInventory::getLocationCode)
				.orderByAsc(WmsPhysicalInventory::getSkuCode)
				.orderByAsc(WmsPhysicalInventory::getInboundDate)
				.orderByAsc(WmsPhysicalInventory::getPickOrder));
	}

	/**
	 * 取某聚合键（货主×服务商×仓×SKU）下的全部批次，用于同事务聚合刷新。
	 */
	default List<WmsPhysicalInventory> listForAggregate(Long wmsTenantId, Long erpTenantId, Long warehouseId,
			String skuCode) {
		return this.selectList(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
			.eq(WmsPhysicalInventory::getWmsTenantId, wmsTenantId)
			.eq(WmsPhysicalInventory::getErpTenantId, erpTenantId)
			.eq(WmsPhysicalInventory::getWarehouseId, warehouseId)
			.eq(WmsPhysicalInventory::getSkuCode, skuCode));
	}

	/**
	 * M-1：取某 (货主×仓×SKU) 下的全部批次（<b>忽略服务商 wms 维</b>），用于货主级快照聚合。
	 * <p>wms_inventory 快照按 (erp,wh,sku) 收敛为 wms=0（与过账引擎 getOrCreate 口径统一），
	 * 须聚合该货主该 SKU 跨全部货架的批次、不按 wms 切分，否则未来出现非 0 批次时会用子集覆盖快照、分裂成两行。
	 */
	default List<WmsPhysicalInventory> listForAggregateByErp(Long erpTenantId, Long warehouseId, String skuCode) {
		return this.selectList(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
			.eq(WmsPhysicalInventory::getErpTenantId, erpTenantId)
			.eq(WmsPhysicalInventory::getWarehouseId, warehouseId)
			.eq(WmsPhysicalInventory::getSkuCode, skuCode));
	}

	default List<WmsPhysicalInventory> listByPalletId(Long palletId) {
		return this.selectList(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
			.eq(WmsPhysicalInventory::getPalletId, palletId)
			.gt(WmsPhysicalInventory::getQuantity, 0));
	}

	/**
	 * 计算同一聚合键 + 入库日的已有批次数，用于生成下一个 pick_order（同日 FIFO 次序）。
	 */
	default int countSameDay(Long wmsTenantId, Long erpTenantId, Long warehouseId, String skuCode,
			LocalDate inboundDate) {
		return Math.toIntExact(this.selectCount(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
			.eq(WmsPhysicalInventory::getWmsTenantId, wmsTenantId)
			.eq(WmsPhysicalInventory::getErpTenantId, erpTenantId)
			.eq(WmsPhysicalInventory::getWarehouseId, warehouseId)
			.eq(WmsPhysicalInventory::getSkuCode, skuCode)
			.eq(WmsPhysicalInventory::getInboundDate, inboundDate)));
	}

	/**
	 * 统计某仓库现存批次数（重新生成库位前的占用校验：>0 表示已有货物落位，禁止清库位重建）。
	 * <p>只数 {@code quantity > 0} 的批次：ship 扣到 0 不删行，残留 0 量行不算占用，否则出库清空后
	 * 永远无法重建库位（与 BUG#1 同根，口径须与 listOccupiedLocationCodes/仓储概览统一）。
	 * @param warehouseId 仓库ID
	 * @return 批次数
	 */
	default long countByWarehouse(Long warehouseId) {
		return this.selectCount(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
			.eq(WmsPhysicalInventory::getWarehouseId, warehouseId)
			.gt(WmsPhysicalInventory::getQuantity, 0));
	}

	/**
	 * 查询某仓库指定库位（编码集合）下的现存批次——库位改分区时联动更新其 zone_id/allocatable。
	 * @param warehouseId 仓库ID
	 * @param locationCodes 库位编码集合
	 * @return 批次列表
	 */
	default java.util.List<WmsPhysicalInventory> listByWarehouseAndLocationCodes(Long warehouseId,
			java.util.Collection<String> locationCodes) {
		if (locationCodes == null || locationCodes.isEmpty()) {
			return java.util.Collections.emptyList();
		}
		return this.selectList(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
			.eq(WmsPhysicalInventory::getWarehouseId, warehouseId)
			.in(WmsPhysicalInventory::getLocationCode, locationCodes));
	}

	/**
	 * 某仓库当前"已占用"的库位编码集合（存在剩余数量的批次才算占用；库位独占校验用）。
	 * <p>口径必须与仓储概览/FIFO 一致：批次 quantity 扣到 0 后（ship 不删行）该库位应视为空闲，
	 * 否则出库清零的库位会被永久判为占用、再也无法上架。故加 {@code quantity > 0} 过滤。
	 * @param warehouseId 仓库ID
	 * @return 去重后的已占用库位编码
	 */
	default List<String> listOccupiedLocationCodes(Long warehouseId) {
		return this.selectList(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
			.select(WmsPhysicalInventory::getLocationCode)
			.eq(WmsPhysicalInventory::getWarehouseId, warehouseId)
			.gt(WmsPhysicalInventory::getQuantity, 0)
			.isNotNull(WmsPhysicalInventory::getLocationCode))
			.stream()
			.map(WmsPhysicalInventory::getLocationCode)
			.filter(c -> c != null && !c.isEmpty())
			.distinct()
			.collect(java.util.stream.Collectors.toList());
	}

	default List<String> listOccupiedPhysicalLocationCodes(Long warehouseId) {
		return this.selectList(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
			.select(WmsPhysicalInventory::getLocationCode)
			.eq(WmsPhysicalInventory::getWarehouseId, warehouseId)
			.and(q -> q.ne(WmsPhysicalInventory::getContainerStored, 1)
				.or().isNull(WmsPhysicalInventory::getContainerStored))
			.gt(WmsPhysicalInventory::getQuantity, 0)
			.isNotNull(WmsPhysicalInventory::getLocationCode))
			.stream()
			.map(WmsPhysicalInventory::getLocationCode)
			.filter(c -> c != null && !c.isEmpty())
			.distinct()
			.collect(java.util.stream.Collectors.toList());
	}

	/**
	 * FIFO 可分配批次（良品 + allocatable，按 inbound_date→pick_order→id 升序）。用于下架预览（只读不锁）。
	 * <p>集装箱存储（口径A）：排除 {@code container_stored=1} 的批次——它们仍计入货主可用(allocatable=1)，
	 * 但已被平台收纳进隐藏集装箱，不参与自动挑拣，避免刚入箱的积压货被 FIFO 优先发掉。
	 */
	default List<WmsPhysicalInventory> selectFifoAllocatable(Long erpTenantId, Long warehouseId,
			String skuCode) {
		return this.selectList(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
			.eq(WmsPhysicalInventory::getErpTenantId, erpTenantId)
			.eq(WmsPhysicalInventory::getWarehouseId, warehouseId)
			.eq(WmsPhysicalInventory::getSkuCode, skuCode)
			.eq(WmsPhysicalInventory::getQuality, "GOOD")
			.eq(WmsPhysicalInventory::getAllocatable, 1)
			.ne(WmsPhysicalInventory::getContainerStored, 1)
			.orderByAsc(WmsPhysicalInventory::getInboundDate)
			.orderByAsc(WmsPhysicalInventory::getPickOrder)
			.orderByAsc(WmsPhysicalInventory::getId));
	}

	/**
	 * FIFO 可分配批次（行锁 SELECT ... FOR UPDATE），用于确认下架时真正锁定 reserved_qty，防超卖（C7）。
	 * <p>同样排除 {@code container_stored=1}（口径A，见 {@link #selectFifoAllocatable}）。
	 */
	default List<WmsPhysicalInventory> selectFifoAllocatableForUpdate(Long erpTenantId,
			Long warehouseId, String skuCode) {
		return this.selectList(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
			.eq(WmsPhysicalInventory::getErpTenantId, erpTenantId)
			.eq(WmsPhysicalInventory::getWarehouseId, warehouseId)
			.eq(WmsPhysicalInventory::getSkuCode, skuCode)
			.eq(WmsPhysicalInventory::getQuality, "GOOD")
			.eq(WmsPhysicalInventory::getAllocatable, 1)
			.ne(WmsPhysicalInventory::getContainerStored, 1)
			.orderByAsc(WmsPhysicalInventory::getInboundDate)
			.orderByAsc(WmsPhysicalInventory::getPickOrder)
			.orderByAsc(WmsPhysicalInventory::getId)
			.last("FOR UPDATE"));
	}

	/**
	 * Locks owner-visible stock held in a virtual location. It must be moved to a
	 * physical location before outbound picking.
	 */
	default List<WmsPhysicalInventory> selectVirtualAllocatableForUpdate(Long erpTenantId,
			Long warehouseId, String skuCode) {
		return this.selectList(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
			.eq(WmsPhysicalInventory::getErpTenantId, erpTenantId)
			.eq(WmsPhysicalInventory::getWarehouseId, warehouseId)
			.eq(WmsPhysicalInventory::getSkuCode, skuCode)
			.eq(WmsPhysicalInventory::getQuality, "GOOD")
			.eq(WmsPhysicalInventory::getAllocatable, 1)
			.eq(WmsPhysicalInventory::getContainerStored, 1)
			.orderByAsc(WmsPhysicalInventory::getInboundDate)
			.orderByAsc(WmsPhysicalInventory::getPickOrder)
			.orderByAsc(WmsPhysicalInventory::getId)
			.last("FOR UPDATE"));
	}

	/**
	 * 集装箱入箱候选：某仓库内、可被平台收纳进集装箱的批次（良品 + 可分配 + 未入箱 + 有余量）。
	 */
	default List<WmsPhysicalInventory> listContainerCandidates(Long warehouseId, Long erpTenantId, String skuKeyword) {
		return this.selectList(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
			.eq(WmsPhysicalInventory::getWarehouseId, warehouseId)
			.eq(WmsPhysicalInventory::getQuality, "GOOD")
			.eq(WmsPhysicalInventory::getAllocatable, 1)
			.ne(WmsPhysicalInventory::getContainerStored, 1)
			.gt(WmsPhysicalInventory::getQuantity, 0)
			.eqIfPresent(WmsPhysicalInventory::getErpTenantId, erpTenantId)
			.likeIfPresent(WmsPhysicalInventory::getSkuCode, skuKeyword)
			.orderByAsc(WmsPhysicalInventory::getInboundDate)
			.orderByAsc(WmsPhysicalInventory::getPickOrder)
			.orderByAsc(WmsPhysicalInventory::getId));
	}

	/**
	 * 集装箱存储追踪：列出某仓库（或全部）当前已入集装箱的批次（平台可追踪货去哪了）。
	 */
	default List<WmsPhysicalInventory> listContainerStored(Long warehouseId, Long erpTenantId, String skuKeyword) {
		return this.selectList(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
			.eq(WmsPhysicalInventory::getContainerStored, 1)
			.gt(WmsPhysicalInventory::getQuantity, 0)
			.eqIfPresent(WmsPhysicalInventory::getWarehouseId, warehouseId)
			.eqIfPresent(WmsPhysicalInventory::getErpTenantId, erpTenantId)
			.likeIfPresent(WmsPhysicalInventory::getSkuCode, skuKeyword)
			.orderByDesc(WmsPhysicalInventory::getUpdateTime));
	}

	/**
	 * 按货主（货物归属）查询批次明细，供 ERP/OMS 只读（C4）。
	 */
	default List<WmsPhysicalInventory> listByErpTenant(Long erpTenantId, Long warehouseId, String skuKeyword) {
		return this.selectList(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
			.eq(WmsPhysicalInventory::getErpTenantId, erpTenantId)
			.eqIfPresent(WmsPhysicalInventory::getWarehouseId, warehouseId)
			.likeIfPresent(WmsPhysicalInventory::getSkuCode, skuKeyword)
			.orderByAsc(WmsPhysicalInventory::getInboundDate)
			.orderByAsc(WmsPhysicalInventory::getPickOrder)
			.orderByAsc(WmsPhysicalInventory::getId));
	}

}
