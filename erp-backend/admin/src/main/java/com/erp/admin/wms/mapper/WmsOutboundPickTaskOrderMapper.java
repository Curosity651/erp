package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.WmsOutboundPickTaskOrder;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;

public interface WmsOutboundPickTaskOrderMapper extends ExtendMapper<WmsOutboundPickTaskOrder> {

    default List<WmsOutboundPickTaskOrder> selectByTaskId(Long taskId) {
        return selectList(WrappersX.lambdaQueryX(WmsOutboundPickTaskOrder.class)
                .eq(WmsOutboundPickTaskOrder::getTaskId, taskId)
                .orderByAsc(WmsOutboundPickTaskOrder::getId));
    }

    default WmsOutboundPickTaskOrder selectLatestByOrderId(Long outboundOrderId) {
        return selectOne(WrappersX.lambdaQueryX(WmsOutboundPickTaskOrder.class)
                .eq(WmsOutboundPickTaskOrder::getOutboundOrderId, outboundOrderId)
                .orderByDesc(WmsOutboundPickTaskOrder::getId)
                .last("LIMIT 1"));
    }
}

