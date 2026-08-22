package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.WmsSortSlot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

import java.util.List;

@Mapper
public interface WmsSortSlotMapper extends ExtendMapper<WmsSortSlot> {

	@Select("SELECT * FROM wms_sort_slot WHERE warehouse_id = #{warehouseId} "
			+ "AND slot_status = 'AVAILABLE' ORDER BY slot_code LIMIT #{limit} FOR UPDATE")
	List<WmsSortSlot> selectAvailableForUpdate(@Param("warehouseId") Long warehouseId,
			@Param("limit") int limit);

	@Select("SELECT * FROM wms_sort_slot WHERE package_id = #{packageId} FOR UPDATE")
	WmsSortSlot selectByPackageIdForUpdate(@Param("packageId") Long packageId);

	@Select("SELECT * FROM wms_sort_slot WHERE task_id = #{taskId} ORDER BY slot_code FOR UPDATE")
	List<WmsSortSlot> selectByTaskIdForUpdate(@Param("taskId") Long taskId);

	@Select("SELECT * FROM wms_sort_slot WHERE scan_code = #{scanCode} LIMIT 1")
	WmsSortSlot selectByScanCode(@Param("scanCode") String scanCode);

	@Select("SELECT * FROM wms_sort_slot WHERE slot_code = #{slotCode} "
			+ "AND slot_status != 'AVAILABLE' ORDER BY id")
	List<WmsSortSlot> selectActiveBySlotCode(@Param("slotCode") String slotCode);

}
