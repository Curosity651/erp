package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.dto.StocktakeDTO;
import com.erp.admin.wms.model.entity.StocktakeOrder;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

/**
 * 盘点单模型转换器
 *
 * @author erp
 */
@Mapper
public interface StocktakeConverter {

	StocktakeConverter INSTANCE = Mappers.getMapper(StocktakeConverter.class);

	/**
	 * DTO 转 Entity
	 * @param dto 盘点单DTO
	 * @return StocktakeOrder 盘点单实体
	 */
	@Mapping(target = "blindCount", ignore = true)
	StocktakeOrder dtoToEntity(StocktakeDTO dto);

	/**
	 * 更新实体（忽略空值）
	 * @param dto 盘点单DTO
	 * @param entity 盘点单实体
	 */
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	@Mapping(target = "blindCount", ignore = true)
	void updateEntity(StocktakeDTO dto, @MappingTarget StocktakeOrder entity);

}
