package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import com.erp.admin.wms.model.qo.FulfillmentShippingQuery;
import com.erp.admin.wms.model.vo.FulfillmentShippingOrderVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface WmsFulfillmentOrderMapper extends BaseMapper<WmsFulfillmentOrder> {
	@Update("UPDATE wms_fulfillment_order SET dispatch_status = #{status}, dispatch_error = #{error}, "
			+ "update_time = NOW() WHERE id = #{id} AND deleted = 0")
	int updateDispatchStatus(@Param("id") Long id, @Param("status") String status,
			@Param("error") String error);

	WmsFulfillmentOrder selectBySource(@Param("sourceType") String sourceType,
			@Param("sourceOrderId") Long sourceOrderId);

	WmsFulfillmentOrder selectForUpdate(@Param("id") Long id);

	int transit(@Param("id") Long id, @Param("from") FulfillmentStatus from,
			@Param("to") FulfillmentStatus to);

	int transitWithReason(@Param("id") Long id, @Param("from") FulfillmentStatus from,
			@Param("to") FulfillmentStatus to, @Param("reason") String reason);

	IPage<FulfillmentShippingOrderVO> selectShippingPage(IPage<FulfillmentShippingOrderVO> page,
			@Param("query") FulfillmentShippingQuery query);
}
