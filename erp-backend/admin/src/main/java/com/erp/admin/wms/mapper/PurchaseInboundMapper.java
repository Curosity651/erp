package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.entity.PurchaseInboundOrder;
import com.erp.admin.wms.model.qo.AvailableShippingQO;
import com.erp.admin.wms.model.qo.PurchaseInboundQO;
import com.erp.admin.wms.model.vo.AvailableShippingVO;
import com.erp.admin.wms.model.vo.PurchaseInboundDetailVO;
import com.erp.admin.wms.model.vo.PurchaseInboundPageVO;
import com.erp.admin.wms.model.vo.PurchaseInboundSimpleVO;
import com.erp.admin.wms.model.vo.ShippingItemForInboundVO;
import com.erp.admin.wms.model.vo.ShippingOrderInboundedQuantityVO;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;

/**
 * 采购入库单 Mapper
 *
 * @author erp
 */
public interface PurchaseInboundMapper extends ExtendMapper<PurchaseInboundOrder> {

    /**
     * 分页查询
     * @param page 分页对象
     * @param qo 查询参数
     * @return IPage<PurchaseInboundPageVO> VO分页数据
     */
    IPage<PurchaseInboundPageVO> queryPage(IPage<PurchaseInboundPageVO> page, @Param("qo") PurchaseInboundQO qo);

    /**
     * 查询入库单详情
     * @param id 入库单ID
     * @return PurchaseInboundDetailVO 入库单详情
     */
    PurchaseInboundDetailVO selectDetailById(@Param("id") Long id);

    /**
     * 根据入库单号查询（用于唯一性校验）
     * @param inboundNo 入库单号
     * @param excludeId 排除的ID（编辑时使用）
     * @return PurchaseInboundOrder 入库单
     */
    PurchaseInboundOrder selectByInboundNo(@Param("inboundNo") String inboundNo, @Param("excludeId") Long excludeId);

    PurchaseInboundOrder selectByIdForUpdate(@Param("id") Long id);

    PurchaseInboundOrder selectSubmittedByInboundNo(@Param("inboundNo") String inboundNo);


    /**
     * 查询物流单待入库明细
     * @param shippingOrderId 物流单ID
     * @return List<ShippingItemForInboundVO> 待入库明细列表
     */
    List<ShippingItemForInboundVO> selectShippingItemsForInbound(@Param("shippingOrderId") Long shippingOrderId);

    /**
     * 查询导出列表
     * @param qo 查询参数
     * @return List<PurchaseInboundPageVO> 导出数据列表
     */
    List<PurchaseInboundPageVO> selectListForExport(@Param("qo") PurchaseInboundQO qo);

    /**
     * 根据物流单ID查询关联入库单
     * @param shippingOrderId 物流单ID
     * @return List<PurchaseInboundSimpleVO> 入库单简要信息列表
     */
    List<PurchaseInboundSimpleVO> selectByShippingOrderId(@Param("shippingOrderId") Long shippingOrderId);

    /**
     * 判断物流单是否存在已确认的入库单
     * @param shippingOrderId 物流单ID
     * @return true-存在，false-不存在
     */
    boolean existsConfirmedByShippingOrderId(@Param("shippingOrderId") Long shippingOrderId);

    /**
     * 统计物流单已入库数量（按物流单ID分组）
     * <p>
     * 查询已确认入库的采购入库单，按物流单ID汇总实际入库数量。
     * 用于计算物流单的剩余预计入库数量。
     *
     * @param shippingOrderIds 物流单ID集合
     * @return List<ShippingOrderInboundedQuantityVO> 已入库数量汇总列表
     */
    List<ShippingOrderInboundedQuantityVO> sumInboundedQuantityByShippingOrderIds(
            @Param("shippingOrderIds") Collection<Long> shippingOrderIds);

    /**
     * 入库单状态 CAS：仅当当前状态为 {@code expect} 时原子改为 {@code next}。
     * <p>用于上架入口消灭并发 TOCTOU（并发/重试上架只有一个赢家推进）。PurchaseInboundOrder 无 @Version，
     * 靠 WHERE order_status=expect 的条件更新保证幂等；调用方须校验返回值==1。
     * @return 受影响行数（1=抢占成功，0=状态已变/并发已被他人推进）
     */
    default int casOrderStatus(Long id, String expect, String next) {
        return this.update(null, WrappersX.lambdaUpdate(PurchaseInboundOrder.class)
                .set(PurchaseInboundOrder::getOrderStatus, next)
                .eq(PurchaseInboundOrder::getId, id)
                .eq(PurchaseInboundOrder::getOrderStatus, expect));
    }

    default int recordPutawayOperator(Long id, Long userId, LocalDateTime putawayTime) {
        return this.update(null, WrappersX.lambdaUpdate(PurchaseInboundOrder.class)
                .set(PurchaseInboundOrder::getPutawayBy, userId)
                .set(PurchaseInboundOrder::getPutawayTime, putawayTime)
                .eq(PurchaseInboundOrder::getId, id)
                .eq(PurchaseInboundOrder::getOrderStatus, "COMPLETED"));
    }

}
