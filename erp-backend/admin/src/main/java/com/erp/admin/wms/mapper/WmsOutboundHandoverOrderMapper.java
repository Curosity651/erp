package com.erp.admin.wms.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.entity.WmsOutboundHandoverOrder;
import com.erp.admin.wms.model.qo.FulfillmentShippingQuery;
import com.erp.admin.wms.model.vo.FulfillmentShippingOrderVO;
import com.erp.admin.wms.model.vo.OutboundHandoverOrderVO;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

public interface WmsOutboundHandoverOrderMapper extends ExtendMapper<WmsOutboundHandoverOrder> {
	IPage<OutboundHandoverOrderVO> selectHandoverPage(IPage<OutboundHandoverOrderVO> page,
			@Param("query") FulfillmentShippingQuery query, @Param("tenantId") Long tenantId);

	OutboundHandoverOrderVO selectHandoverDetail(@Param("id") Long id,
			@Param("tenantId") Long tenantId);

	List<FulfillmentShippingOrderVO> selectAvailableFulfillments(@Param("tenantId") Long tenantId);

	List<String> selectRecentDestinations(@Param("tenantId") Long tenantId);

	WmsOutboundHandoverOrder selectForUpdate(@Param("id") Long id,
			@Param("tenantId") Long tenantId);
}
