package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.WmsOutboundPickAllocation;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;
import java.util.Collection;

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

    default List<WmsOutboundPickAllocation> selectByOrdersAndInventory(
            Collection<Long> outboundOrderIds, Long physicalInventoryId) {
        if (outboundOrderIds == null || outboundOrderIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectList(WrappersX.lambdaQueryX(WmsOutboundPickAllocation.class)
                .in(WmsOutboundPickAllocation::getOutboundOrderId, outboundOrderIds)
                .eq(WmsOutboundPickAllocation::getPhysicalInventoryId, physicalInventoryId)
                .orderByAsc(WmsOutboundPickAllocation::getId)
                .last("FOR UPDATE"));
    }

}
