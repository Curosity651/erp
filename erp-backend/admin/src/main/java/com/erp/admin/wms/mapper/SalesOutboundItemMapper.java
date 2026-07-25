package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.SalesOutboundOrderItem;
import com.erp.admin.wms.model.vo.SalesOutboundItemVO;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 销售出库单明细 Mapper
 *
 * @author erp
 */
public interface SalesOutboundItemMapper extends ExtendMapper<SalesOutboundOrderItem> {

    @Select("SELECT * FROM wms_sales_outbound_order_item WHERE outbound_order_id = #{outboundOrderId} "
            + "AND erp_order_id = #{erpOrderId} ORDER BY id FOR UPDATE")
    List<SalesOutboundOrderItem> selectPackageItemsForUpdate(
            @Param("outboundOrderId") Long outboundOrderId, @Param("erpOrderId") Long erpOrderId);

    /**
     * 根据出库单ID查询明细
     * @param outboundOrderId 出库单ID
     * @return List<SalesOutboundOrderItem> 明细列表
     */
    default List<SalesOutboundOrderItem> selectByOutboundOrderId(Long outboundOrderId) {
        return selectList(WrappersX.<SalesOutboundOrderItem>lambdaQueryX()
                .eq(SalesOutboundOrderItem::getOutboundOrderId, outboundOrderId)
                .orderByAsc(SalesOutboundOrderItem::getId));
    }

    /**
     * 根据出库单ID删除明细
     * @param outboundOrderId 出库单ID
     * @return 影响行数
     */
    default int deleteByOutboundOrderId(Long outboundOrderId) {
        return delete(WrappersX.<SalesOutboundOrderItem>lambdaQueryX()
                .eq(SalesOutboundOrderItem::getOutboundOrderId, outboundOrderId));
    }

}
