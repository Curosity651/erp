package com.erp.admin.wms.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Checks whether a virtual location is still referenced by warehouse documents.
 */
public interface VirtualLocationReferenceMapper {

	@Select({ "SELECT COUNT(*)",
			"FROM wms_location_transfer_item i",
			"JOIN wms_location_transfer_order o ON o.id = i.transfer_order_id",
			"WHERE o.deleted = 0",
			"AND o.warehouse_id = #{warehouseId}",
			"AND o.order_status IN ('PLANNED', 'PENDING')",
			"AND (i.source_location_code = #{locationCode} OR i.target_location_code = #{locationCode})" })
	long countUnfinishedTransfers(@Param("warehouseId") Long warehouseId,
			@Param("locationCode") String locationCode);

	@Select({ "SELECT",
			"(SELECT COUNT(*) FROM wms_location_transfer_item i",
			" JOIN wms_location_transfer_order o ON o.id = i.transfer_order_id",
			" WHERE o.deleted = 0 AND o.warehouse_id = #{warehouseId}",
			" AND (i.source_location_code = #{locationCode} OR i.target_location_code = #{locationCode}))",
			"+ (SELECT COUNT(*) FROM wms_adjustment_order_item i",
			" JOIN wms_adjustment_order o ON o.id = i.adjustment_order_id",
			" WHERE o.deleted = 0 AND o.warehouse_id = #{warehouseId} AND i.location_code = #{locationCode})",
			"+ (SELECT COUNT(*) FROM wms_stocktake_location_task",
			" WHERE warehouse_id = #{warehouseId} AND location_code = #{locationCode})",
			"+ (SELECT COUNT(*) FROM wms_stocktake_order_item i",
			" JOIN wms_stocktake_order o ON o.id = i.stocktake_order_id",
			" WHERE o.deleted = 0 AND o.warehouse_id = #{warehouseId} AND i.location_code = #{locationCode})",
			"+ (SELECT COUNT(*) FROM wms_outbound_pick_allocation i",
			" JOIN wms_sales_outbound_order o ON o.id = i.outbound_order_id",
			" WHERE o.warehouse_id = #{warehouseId} AND i.location_code = #{locationCode})",
			"+ (SELECT COUNT(*) FROM wms_outbound_pick_task_line i",
			" JOIN wms_outbound_pick_task o ON o.id = i.task_id",
			" WHERE o.warehouse_id = #{warehouseId} AND i.location_code = #{locationCode})",
			"+ (SELECT COUNT(*) FROM wms_return_qc_item i",
			" JOIN wms_return_inbound_order o ON o.id = i.return_order_id",
			" WHERE o.deleted = 0 AND o.warehouse_id = #{warehouseId}",
			" AND (i.qualified_location_code = #{locationCode} OR i.damaged_location_code = #{locationCode}",
			" OR i.location_code = #{locationCode}))",
			"+ (SELECT COUNT(*) FROM wms_transfer_order_item i",
			" JOIN wms_transfer_order o ON o.id = i.transfer_order_id",
			" WHERE o.to_warehouse_id = #{warehouseId} AND i.deleted = 0",
			" AND i.target_location_code = #{locationCode})",
			"+ (SELECT COUNT(*) FROM wms_physical_inventory",
			" WHERE warehouse_id = #{warehouseId} AND origin_location_code = #{locationCode})" })
	long countBusinessReferences(@Param("warehouseId") Long warehouseId,
			@Param("locationCode") String locationCode);

}
