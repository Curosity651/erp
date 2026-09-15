package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.PurchaseInboundOrderItem;
import com.erp.admin.wms.model.vo.InboundPurchaseOrderNoVO;
import com.erp.admin.wms.model.vo.PurchaseInboundItemVO;
import com.erp.admin.wms.model.vo.PurchaseInboundStatsVO;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.Collection;
import java.util.List;

/**
 * 采购入库单明细 Mapper
 *
 * @author erp
 */
public interface PurchaseInboundItemMapper extends ExtendMapper<PurchaseInboundOrderItem> {

    /**
     * 批量查询入库单商品摘要及统计
     * @param orderIds 入库单ID列表
     * @return List<PurchaseInboundStatsVO> 统计信息列表
     */
    List<PurchaseInboundStatsVO> selectItemSummaryByOrderIds(@Param("orderIds") Collection<Long> orderIds);

    /**
     * 批量查询入库单关联的采购单号
     * @param orderIds 入库单ID列表
     * @return List<InboundPurchaseOrderNoVO> 采购单号映射列表
     */
    List<InboundPurchaseOrderNoVO> selectPurchaseOrderNosByOrderIds(@Param("orderIds") Collection<Long> orderIds);

    /**
     * 根据入库单ID查询明细列表
     * @param inboundOrderId 入库单ID
     * @return List<PurchaseInboundOrderItem> 明细列表
     */
    default List<PurchaseInboundOrderItem> selectByInboundOrderId(Long inboundOrderId) {
        LambdaQueryWrapperX<PurchaseInboundOrderItem> wrapper = WrappersX.lambdaQueryX(PurchaseInboundOrderItem.class)
            .eq(PurchaseInboundOrderItem::getInboundOrderId, inboundOrderId)
            .orderByAsc(PurchaseInboundOrderItem::getId);
        return this.selectList(wrapper);
    }

    /**
     * 根据入库单ID删除明细
     * @param inboundOrderId 入库单ID
     * @return 影响行数
     */
    default int deleteByInboundOrderId(Long inboundOrderId) {
        LambdaQueryWrapperX<PurchaseInboundOrderItem> wrapper = WrappersX.lambdaQueryX(PurchaseInboundOrderItem.class)
            .eq(PurchaseInboundOrderItem::getInboundOrderId, inboundOrderId);
        return this.delete(wrapper);
    }

    /**
     * 查询入库单明细VO列表（含采购单号、SKU名称）
     * @param inboundOrderId 入库单ID
     * @param erpTenantId 货主租户ID，用于按货主读取 SKU 包装尺寸
     * @return List<PurchaseInboundItemVO> 明细VO列表
     */
    List<PurchaseInboundItemVO> selectItemVOsByInboundOrderId(
            @Param("inboundOrderId") Long inboundOrderId, @Param("erpTenantId") Long erpTenantId);

}
