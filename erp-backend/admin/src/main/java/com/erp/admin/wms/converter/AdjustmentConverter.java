package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.dto.AdjustmentDTO;
import com.erp.admin.wms.model.entity.AdjustmentOrder;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

/**
 * 调整单模型转换器
 *
 * @author erp
 */
@Mapper
public interface AdjustmentConverter {

	AdjustmentConverter INSTANCE = Mappers.getMapper(AdjustmentConverter.class);

	/**
	 * DTO 转 Entity
	 * @param dto 调整单DTO
	 * @return AdjustmentOrder 调整单实体
	 */
	AdjustmentOrder dtoToEntity(AdjustmentDTO dto);

	/**
	 * 更新实体（忽略空值）
	 * @param dto 调整单DTO
	 * @param entity 调整单实体
	 */
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateEntity(AdjustmentDTO dto, @MappingTarget AdjustmentOrder entity);

}
