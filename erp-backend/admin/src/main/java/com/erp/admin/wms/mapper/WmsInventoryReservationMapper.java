package com.erp.admin.wms.mapper;

import java.util.List;

import com.erp.admin.wms.model.entity.WmsInventoryReservation;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

public interface WmsInventoryReservationMapper extends ExtendMapper<WmsInventoryReservation> {

	List<WmsInventoryReservation> selectReservedForUpdate(@Param("fulfillmentOrderId") Long fulfillmentOrderId);

	int markStatus(@Param("id") Long id, @Param("fromStatus") String fromStatus,
			@Param("toStatus") String toStatus, @Param("version") Integer version);

}
