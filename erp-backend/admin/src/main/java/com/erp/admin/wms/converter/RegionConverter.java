package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.dto.RegionDTO;
import com.erp.admin.wms.model.entity.Region;
import com.erp.admin.wms.model.vo.RegionOptionVO;
import com.erp.admin.wms.model.vo.RegionPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface RegionConverter {

    RegionConverter INSTANCE = Mappers.getMapper(RegionConverter.class);

    Region dtoToEntity(RegionDTO dto);

    RegionPageVO poToPageVo(Region region);

    RegionOptionVO poToOptionVo(Region region);
}
