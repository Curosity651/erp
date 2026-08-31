package com.erp.admin.wms.mapper;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface WmsFulfillmentPickTaskMapper extends BaseMapper<WmsFulfillmentPickTask> {
	@Update("UPDATE wms_fulfillment_pick_task SET task_status = 'PICKING', "
			+ "operator_id = #{userId}, claimed_time = #{claimedTime}, update_time = NOW() "
			+ "WHERE id = #{id} AND task_status = 'PENDING' AND operator_id IS NULL")
	int claim(@Param("id") Long id, @Param("userId") Long userId,
			@Param("claimedTime") LocalDateTime claimedTime);

	@Update("UPDATE wms_fulfillment_pick_task SET task_status = 'PENDING', "
			+ "operator_id = NULL, claimed_time = NULL, update_time = NOW() "
			+ "WHERE id = #{id} AND operator_id = #{userId} "
			+ "AND task_status IN ('PICKING', 'PARTIAL_EXCEPTION')")
	int release(@Param("id") Long id, @Param("userId") Long userId);

	@Update("UPDATE wms_fulfillment_pick_task SET operator_id = #{targetUserId}, "
			+ "claimed_time = #{claimedTime}, task_status = 'PICKING', update_time = NOW() "
			+ "WHERE id = #{id} AND task_status IN ('PENDING', 'PICKING', 'PARTIAL_EXCEPTION')")
	int transfer(@Param("id") Long id, @Param("targetUserId") Long targetUserId,
			@Param("claimedTime") LocalDateTime claimedTime);

	@Update("UPDATE wms_fulfillment_pick_task SET operation_mode = #{mode}, update_time = NOW() "
			+ "WHERE id = #{id} AND (operation_mode IS NULL OR operation_mode = #{mode})")
	int claimOperationMode(@Param("id") Long id, @Param("mode") String mode);
}
