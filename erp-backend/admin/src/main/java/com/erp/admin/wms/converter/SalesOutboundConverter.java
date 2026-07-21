package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.dto.SalesOutboundDTO;
import com.erp.admin.wms.model.entity.SalesOutboundOrder;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

/**
 * 销售出库单模型转换器
 *
 * @author erp
 */
@Mapper
public interface SalesOutboundConverter {

    SalesOutboundConverter INSTANCE = Mappers.getMapper(SalesOutboundConverter.class);

    /**
     * DTO 转 Entity
     * @param dto 销售出库单DTO
     * @return SalesOutboundOrder 销售出库单实体
     */
    SalesOutboundOrder dtoToEntity(SalesOutboundDTO dto);

    /**
     * 更新实体（忽略空值）
     * @param dto 销售出库单DTO
     * @param entity 销售出库单实体
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(SalesOutboundDTO dto, @MappingTarget SalesOutboundOrder entity);

}
