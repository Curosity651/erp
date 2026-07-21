package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.entity.Inventory;
import com.erp.admin.wms.model.vo.InventoryDetailVO;
import com.erp.admin.wms.model.vo.InventoryPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 库存 Converter
 *
 * @author erp
 */
@Mapper
public interface InventoryConverter {

    InventoryConverter INSTANCE = Mappers.getMapper(InventoryConverter.class);

    /**
     * Entity -> PageVO
     */
    InventoryPageVO entityToPageVO(Inventory entity);

    /**
     * Entity List -> PageVO List
     */
    List<InventoryPageVO> entityListToPageVOList(List<Inventory> entities);

    /**
     * Entity -> DetailVO
     */
    InventoryDetailVO entityToDetailVO(Inventory entity);

}
