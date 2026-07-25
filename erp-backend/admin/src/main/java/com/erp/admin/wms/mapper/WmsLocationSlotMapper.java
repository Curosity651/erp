package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.WmsLocationSlot;
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
}
