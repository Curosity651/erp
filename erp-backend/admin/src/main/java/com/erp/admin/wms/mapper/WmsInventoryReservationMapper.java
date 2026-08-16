package com.erp.admin.wms.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.admin.wms.model.entity.WmsInventoryReservation;
import org.apache.ibatis.annotations.Param;

public interface WmsInventoryReservationMapper extends BaseMapper<WmsInventoryReservation> {
	List<WmsInventoryReservation> selectReservedForUpdate(@Param("fulfillmentOrderId") Long fulfillmentOrderId);

	int markStatus(@Param("id") Long id, @Param("from") String from, @Param("to") String to,
			@Param("version") Integer version);
}
