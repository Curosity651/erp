package com.erp.admin.wms.model.converter;

import java.util.List;

import com.erp.admin.wms.model.dto.InventoryConfigDTO;
import com.erp.admin.wms.model.entity.InventoryConfig;
import com.erp.admin.wms.model.vo.InventoryConfigVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 库存配置转换器
 *
 * @author erp
 */
@Mapper
public interface InventoryConfigConverter {

    InventoryConfigConverter INSTANCE = Mappers.getMapper(InventoryConfigConverter.class);

    InventoryConfigVO entityToVo(InventoryConfig entity);

    InventoryConfig dtoToEntity(InventoryConfigDTO dto);

	List<InventoryConfigVO> entityToVos(List<InventoryConfig> records);
}
