package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.entity.StockPosting;
import com.erp.admin.wms.model.entity.StockPostingItem;
import com.erp.admin.wms.model.vo.StockPostingDetailVO;
import com.erp.admin.wms.model.vo.StockPostingItemVO;
import com.erp.admin.wms.model.vo.StockPostingPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 库存过账单转换器
 *
 * @author erp
 */
@Mapper
public interface StockPostingConverter {

    StockPostingConverter INSTANCE = Mappers.getMapper(StockPostingConverter.class);

    /**
     * Entity 转 PageVO
     */
    StockPostingPageVO poToPageVo(StockPosting entity);

    /**
     * Entity 转 DetailVO
     */
    StockPostingDetailVO poToDetailVo(StockPosting entity);

    /**
     * 明细 Entity 转 VO
     */
    StockPostingItemVO itemToVo(StockPostingItem item);

}
