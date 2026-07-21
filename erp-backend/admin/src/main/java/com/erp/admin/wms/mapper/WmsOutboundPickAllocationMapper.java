package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.WmsOutboundPickAllocation;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;

/**
 * 出库下架 FIFO 分配 Mapper。
 *
 * @author erp
 */
public interface WmsOutboundPickAllocationMapper extends ExtendMapper<WmsOutboundPickAllocation> {

    /** 按出库单查分配明细（拣货单），按库位排序 */
    default List<WmsOutboundPickAllocation> selectByOutboundOrderId(Long outboundOrderId) {
        return this.selectList(WrappersX.lambdaQueryX(WmsOutboundPickAllocation.class)
            .eq(WmsOutboundPickAllocation::getOutboundOrderId, outboundOrderId)
            .orderByAsc(WmsOutboundPickAllocation::getLocationCode)
            .orderByAsc(WmsOutboundPickAllocation::getInboundDate)
            .orderByAsc(WmsOutboundPickAllocation::getPickOrder));
    }

}
