package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.admin.wms.model.entity.WmsFulfillmentPlatformAction;
import org.apache.ibatis.annotations.Param;

public interface WmsFulfillmentPlatformActionMapper extends BaseMapper<WmsFulfillmentPlatformAction> {
	WmsFulfillmentPlatformAction selectForUpdate(@Param("fulfillmentOrderId") Long fulfillmentOrderId,
			@Param("actionType") String actionType);
}
