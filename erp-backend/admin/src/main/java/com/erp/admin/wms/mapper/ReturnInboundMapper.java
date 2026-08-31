package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.ReturnInboundOrder;
import com.erp.admin.wms.model.qo.ReturnInboundQO;
import com.erp.admin.wms.model.qo.ReturnableOrderQO;
import com.erp.admin.wms.model.vo.ReturnInboundDetailVO;
import com.erp.admin.wms.model.vo.ReturnInboundExportVO;
import com.erp.admin.wms.model.vo.ReturnInboundPageVO;
import com.erp.admin.wms.model.vo.ReturnableOrderVO;
import com.erp.admin.wms.model.vo.ReturnOrderSourceVO;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

/**
 * 退货入库单 Mapper
 *
 * @author erp
 */
public interface ReturnInboundMapper extends ExtendMapper<ReturnInboundOrder> {

    /**
     * 分页查询
     * @param page 分页对象
     * @param qo 查询参数
     * @return IPage<ReturnInboundPageVO> VO分页数据
     */
    IPage<ReturnInboundPageVO> queryPage(IPage<ReturnInboundPageVO> page, @Param("qo") ReturnInboundQO qo);

    /**
     * 查询退货单详情
     * @param id 退货单ID
     * @return ReturnInboundDetailVO 退货单详情
     */
    ReturnInboundDetailVO selectDetailById(@Param("id") Long id);

    /**
     * 根据退货单号查询（用于唯一性校验）
     * @param returnNo 退货单号
     * @param excludeId 排除的ID
     * @return ReturnInboundOrder 退货单
     */
    default ReturnInboundOrder selectByReturnNo(String returnNo, Long excludeId) {
        return selectOne(WrappersX.<ReturnInboundOrder>lambdaQueryX()
                .eq(ReturnInboundOrder::getReturnNo, returnNo)
                .neIfPresent(ReturnInboundOrder::getId, excludeId));
    }

    /**
     * 分页查询可退货订单列表
     * 查询已出库（outbound_status = 'SHIPPED'）的订单
     * @param page 分页对象
     * @param qo 查询参数
     * @return IPage<ReturnableOrderVO> 可退货订单分页数据
     */
    IPage<ReturnableOrderVO> selectReturnableOrders(IPage<ReturnableOrderVO> page, @Param("qo") ReturnableOrderQO qo);

    /** Locks the linked order item and returns fields derived from the database. */
    ReturnOrderSourceVO selectSourceForUpdate(@Param("orderItemId") Long orderItemId,
                                               @Param("erpTenantId") Long erpTenantId);

    /**
     * 查询导出列表
     * @param qo 查询参数
     * @return List<ReturnInboundExportVO> 导出数据列表
     */
    List<ReturnInboundExportVO> selectListForExport(@Param("qo") ReturnInboundQO qo);

    /**
     * 退货单状态 CAS：仅当当前状态为 {@code expect} 时原子改为 {@code next}。
     * <p>用于收货/质检入口消灭并发 TOCTOU（并发/双击提交只有一个赢家推进）。ReturnInboundOrder 无 @Version，
     * 靠 WHERE return_status=expect 的条件更新保证幂等；调用方须校验返回值==1。
     * @return 受影响行数（1=抢占成功，0=状态已变/并发已被他人推进）
     */
    default int casReturnStatus(Long id, String expect, String next) {
        return this.update(null, WrappersX.lambdaUpdate(ReturnInboundOrder.class)
                .set(ReturnInboundOrder::getReturnStatus, next)
                .eq(ReturnInboundOrder::getId, id)
                .eq(ReturnInboundOrder::getReturnStatus, expect));
    }

    default int markReceivedAudit(Long id, Long userId) {
        return this.update(null, WrappersX.lambdaUpdate(ReturnInboundOrder.class)
                .set(ReturnInboundOrder::getReceivedBy, userId)
                .set(ReturnInboundOrder::getReceivedTime, LocalDateTime.now())
                .eq(ReturnInboundOrder::getId, id)
                .eq(ReturnInboundOrder::getReturnStatus, "QC_PENDING"));
    }

    default int markClosedAudit(Long id, Long userId) {
        return this.update(null, WrappersX.lambdaUpdate(ReturnInboundOrder.class)
                .set(ReturnInboundOrder::getClosedBy, userId)
                .set(ReturnInboundOrder::getClosedTime, LocalDateTime.now())
                .eq(ReturnInboundOrder::getId, id)
                .eq(ReturnInboundOrder::getReturnStatus, "CLOSED"));
    }

}
