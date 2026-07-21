package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.dto.ShippingOrderItemDTO;
import com.erp.admin.wms.model.entity.ShippingOrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 物流单明细模型转换器
 *
 * @author erp
 */
@Mapper
public interface ShippingOrderItemConverter {

	ShippingOrderItemConverter INSTANCE = Mappers.getMapper(ShippingOrderItemConverter.class);

	/**
	 * DTO 转 Entity
	 * @param dto 物流单明细DTO
	 * @return ShippingOrderItem 物流单明细实体
	 */
	ShippingOrderItem dtoToEntity(ShippingOrderItemDTO dto);

	/**
	 * DTO列表 转 Entity列表
	 * @param dtoList 物流单明细DTO列表
	 * @return List<ShippingOrderItem> 物流单明细实体列表
	 */
	List<ShippingOrderItem> dtoListToEntityList(List<ShippingOrderItemDTO> dtoList);

}
