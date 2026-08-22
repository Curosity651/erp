package com.erp.admin.wms.mapper;

import java.math.BigDecimal;
import java.util.List;

import com.erp.admin.wms.model.entity.WmsLocationSlot;
import com.erp.admin.wms.model.vo.LocationSlotLevelSummaryVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

@Mapper
public interface WmsLocationSlotMapper extends ExtendMapper<WmsLocationSlot> {

    @Select("SELECT * FROM wms_location_slot WHERE id = #{id} FOR UPDATE")
    WmsLocationSlot selectForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM wms_location_slot "
            + "WHERE warehouse_id = #{warehouseId} AND location_id = #{locationId} "
            + "AND slot_status = 'EMPTY' ORDER BY level_no, position_no LIMIT 1 FOR UPDATE")
    WmsLocationSlot selectFirstEmptyForUpdate(@Param("warehouseId") Long warehouseId,
            @Param("locationId") Long locationId);

    @Update("UPDATE wms_location_slot SET slot_status='OCCUPIED', version=version+1 " +
            "WHERE id=#{id} AND slot_status='EMPTY'")
    int claim(@Param("id") Long id);

    @Update("UPDATE wms_location_slot SET slot_status='EMPTY', version=version+1 " +
            "WHERE id=#{id} AND slot_status='OCCUPIED'")
    int release(@Param("id") Long id);

    @Delete("DELETE s FROM wms_location_slot s " +
            "JOIN wms_location l ON l.id=s.location_id " +
            "WHERE l.warehouse_id=#{warehouseId} AND l.is_virtual=0")
    int deletePhysicalSlots(@Param("warehouseId") Long warehouseId);

    @Select("SELECT COUNT(*) FROM wms_location_slot s " +
            "JOIN wms_location l ON l.id=s.location_id " +
            "WHERE l.warehouse_id=#{warehouseId} AND l.is_virtual=0 AND l.deleted=0")
    int countPhysicalSlots(@Param("warehouseId") Long warehouseId);

    @Select("SELECT s.location_id AS locationId, s.level_no AS levelNo, COUNT(*) AS totalCount, "
            + "SUM(CASE WHEN p.id IS NULL THEN 0 ELSE 1 END) AS occupiedCount, "
            + "MAX(CASE WHEN pi.location_code IS NULL THEN 0 ELSE 1 END) AS blocked "
            + "FROM wms_location_slot s "
            + "JOIN wms_location l ON l.id = s.location_id AND l.deleted = 0 AND l.is_virtual = 0 "
            + "LEFT JOIN wms_pallet p ON p.current_slot_id = s.id "
            + "LEFT JOIN (SELECT warehouse_id, location_code FROM wms_physical_inventory "
            + "WHERE COALESCE(quantity, 0) > 0 OR COALESCE(reserved_qty, 0) > 0 "
            + "GROUP BY warehouse_id, location_code) pi "
            + "ON pi.warehouse_id = s.warehouse_id AND pi.location_code = l.location_code "
            + "WHERE s.warehouse_id = #{warehouseId} "
            + "GROUP BY s.location_id, s.level_no "
            + "ORDER BY s.location_id, s.level_no DESC")
    List<LocationSlotLevelSummaryVO> listLevelSummaries(@Param("warehouseId") Long warehouseId);

    @Update("UPDATE wms_location_slot SET max_weight_kg = #{maxWeightKg}, update_time = NOW() "
            + "WHERE warehouse_id = #{warehouseId}")
    int updateMaxWeightByWarehouse(@Param("warehouseId") Long warehouseId,
            @Param("maxWeightKg") BigDecimal maxWeightKg);

    @Insert("<script>"
            + "INSERT INTO wms_location_slot "
            + "(warehouse_id, location_id, level_no, position_no, slot_code, max_height_mm, "
            + "max_weight_kg, slot_status, version) VALUES "
            + "<foreach collection='slots' item='slot' separator=','>"
            + "(#{slot.warehouseId}, #{slot.locationId}, #{slot.levelNo}, #{slot.positionNo}, "
            + "#{slot.slotCode}, #{slot.maxHeightMm}, #{slot.maxWeightKg}, #{slot.slotStatus}, #{slot.version})"
            + "</foreach>"
            + "</script>")
    int insertBatch(@Param("slots") List<WmsLocationSlot> slots);
}
