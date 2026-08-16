package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import org.apache.ibatis.annotations.Param;

public interface WmsFulfillmentOrderMapper extends BaseMapper<WmsFulfillmentOrder> {
	WmsFulfillmentOrder selectBySource(@Param("sourceType") String sourceType,
			@Param("sourceOrderId") Long sourceOrderId);

	WmsFulfillmentOrder selectForUpdate(@Param("id") Long id);

	int transit(@Param("id") Long id, @Param("from") FulfillmentStatus from,
			@Param("to") FulfillmentStatus to);
}
