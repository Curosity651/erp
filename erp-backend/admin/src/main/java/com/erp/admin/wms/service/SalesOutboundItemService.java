package com.erp.admin.wms.service;

import com.erp.admin.wms.converter.SalesOutboundItemConverter;
import com.erp.admin.wms.mapper.SalesOutboundItemMapper;
import com.erp.admin.wms.model.dto.SalesOutboundItemDTO;
import com.erp.admin.wms.model.entity.SalesOutboundOrderItem;
import com.erp.admin.wms.model.vo.SalesOutboundItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 销售出库单明细服务
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesOutboundItemService extends ExtendServiceImpl<SalesOutboundItemMapper, SalesOutboundOrderItem> {

    /**
     * 根据出库单ID查询明细
     * @param outboundOrderId 出库单ID
     * @return List<SalesOutboundOrderItem> 明细列表
     */
    public List<SalesOutboundOrderItem> getByOutboundOrderId(Long outboundOrderId) {
        return baseMapper.selectByOutboundOrderId(outboundOrderId);
    }

    /**
     * 批量保存明细
     * @param outboundOrderId 出库单ID
     * @param items 明细DTO列表
     */
    public void batchSave(Long outboundOrderId, List<SalesOutboundItemDTO> items) {
        List<SalesOutboundOrderItem> entities = SalesOutboundItemConverter.INSTANCE.dtoListToEntityList(items);
        for (SalesOutboundOrderItem entity : entities) {
            entity.setOutboundOrderId(outboundOrderId);
        }
        this.saveBatch(entities);
    }

    /**
     * 根据出库单ID删除明细
     * @param outboundOrderId 出库单ID
     */
    public void deleteByOutboundOrderId(Long outboundOrderId) {
        baseMapper.deleteByOutboundOrderId(outboundOrderId);
    }

}
