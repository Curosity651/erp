package com.erp.admin.order.converter;

import com.erp.admin.common.util.TimeZoneUtils;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.vo.OzonOrderPageVO;
import com.erp.admin.order.model.vo.WbOrderPageVO;
import com.erp.admin.order.model.vo.YdOrderPageVO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * 订单主表模型转换器
 *
 * @author erp 2025-09-27 23:16:08
 */
@Mapper
public interface ErpOrderConverter {

	ErpOrderConverter INSTANCE = Mappers.getMapper(ErpOrderConverter.class);

	/**
	 * PO 转 PageVO
	 * @param erpOrder 订单主表
	 * @return ErpOrderPageVO 订单主表PageVO
	 */
	@Mapping(target = "hasLabel", expression="java(erpOrder.getLabelBase64() != null)")
	WbOrderPageVO poToWbPageVo(ErpOrder erpOrder);

	@Mapping(target = "hasLabel", expression="java(erpOrder.getLabelBase64() != null)")
	OzonOrderPageVO poToOzonPageVo(ErpOrder erpOrder);

	@Mapping(target = "hasLabel", expression="java(erpOrder.getLabelBase64() != null)")
	YdOrderPageVO poToYdPageVo(ErpOrder erpOrder);

}
