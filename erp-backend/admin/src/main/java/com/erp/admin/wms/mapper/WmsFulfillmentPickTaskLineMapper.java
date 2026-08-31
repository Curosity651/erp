package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskLine;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface WmsFulfillmentPickTaskLineMapper extends BaseMapper<WmsFulfillmentPickTaskLine> {
	@Update("UPDATE wms_fulfillment_pick_task_line SET "
			+ "line_status = CASE WHEN picked_quantity + #{quantity} = planned_quantity THEN 'COMPLETED' ELSE 'PICKING' END, "
			+ "picked_quantity = picked_quantity + #{quantity}, "
			+ "version = version + 1, update_time = NOW() WHERE id = #{id} AND version = #{version} "
			+ "AND picked_quantity + #{quantity} <= planned_quantity AND line_status IN ('PENDING','PICKING')")
	int addPicked(@Param("id") Long id, @Param("quantity") Integer quantity,
			@Param("version") Integer version);

	@Update("UPDATE wms_fulfillment_pick_task_line SET returned_quantity = returned_quantity + #{quantity}, "
			+ "version = version + 1, update_time = NOW() WHERE id = #{id} AND version = #{version} "
			+ "AND returned_quantity + #{quantity} <= picked_quantity")
	int addReturned(@Param("id") Long id, @Param("quantity") Integer quantity,
			@Param("version") Integer version);

	@Update("UPDATE wms_fulfillment_pick_task_line SET picked_quantity = planned_quantity, "
			+ "line_status = 'COMPLETED', version = version + 1, update_time = NOW() "
			+ "WHERE id = #{id} AND version = #{version} AND picked_quantity = 0 "
			+ "AND line_status = 'PENDING'")
	int completeForSimplifiedTask(@Param("id") Long id, @Param("version") Integer version);
}
