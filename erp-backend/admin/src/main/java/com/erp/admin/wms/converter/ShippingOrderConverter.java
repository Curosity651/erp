package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.dto.ShippingOrderDTO;
import com.erp.admin.wms.model.entity.ShippingOrder;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

/**
 * 物流单模型转换器
 *
 * @author erp
 */
@Mapper
public interface ShippingOrderConverter {

	ShippingOrderConverter INSTANCE = Mappers.getMapper(ShippingOrderConverter.class);

	/**
	 * DTO 转 Entity
	 * @param dto 物流单DTO
	 * @return ShippingOrder 物流单实体
	 */
	ShippingOrder dtoToEntity(ShippingOrderDTO dto);

	/**
	 * 更新实体（忽略空值）
	 * @param dto 物流单DTO
	 * @param entity 物流单实体
	 */
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateEntity(ShippingOrderDTO dto, @MappingTarget ShippingOrder entity);

}
