package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.entity.PlatformRegionMapping;
import com.erp.admin.wms.model.vo.PlatformRegionMappingVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PlatformRegionMappingConverter {

    PlatformRegionMappingConverter INSTANCE = Mappers.getMapper(PlatformRegionMappingConverter.class);

    PlatformRegionMappingVO poToVo(PlatformRegionMapping mapping);
}
