package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.WmsReturnQcItem;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;

/**
 * 退货质检明细 Mapper。
 *
 * @author erp
 */
public interface WmsReturnQcItemMapper extends ExtendMapper<WmsReturnQcItem> {

    /** 按退货单查质检明细 */
    default List<WmsReturnQcItem> selectByReturnOrderId(Long returnOrderId) {
        return this.selectList(WrappersX.lambdaQueryX(WmsReturnQcItem.class)
            .eq(WmsReturnQcItem::getReturnOrderId, returnOrderId)
            .orderByAsc(WmsReturnQcItem::getId));
    }

}
