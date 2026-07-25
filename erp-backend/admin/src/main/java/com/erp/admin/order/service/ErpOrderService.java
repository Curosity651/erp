package com.erp.admin.order.service;

import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.enums.OutboundStatus;
import com.erp.admin.order.model.qo.PendingOrderQO;
import com.erp.admin.order.model.vo.SkuDailySalesVO;
import com.erp.admin.wms.constant.ForecastConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * ERP订单服务
 * <p>
 * 提供订单出库状态管理功能
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErpOrderService extends ExtendServiceImpl<ErpOrderMapper, ErpOrder> {

    /**
     * 更新订单出库状态
     *
     * @param id 订单ID
     * @param outboundStatus 出库状态
     * @param outboundOrderId 出库单ID（可选）
     * @param outboundTime 出库时间（可选）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOutboundStatus(Long id, OutboundStatus outboundStatus, Long outboundOrderId, LocalDateTime outboundTime) {
        Assert.notNull(id, "订单ID不能为空");
        Assert.notNull(outboundStatus, "出库状态不能为空");

        int affected = baseMapper.updateOutboundStatus(id, outboundStatus.name(), outboundOrderId, outboundTime);
        Assert.isTrue(affected > 0, "订单不存在或更新失败，订单ID: " + id);

        log.info("Updated order outbound status, orderId={}, outboundStatus={}, outboundOrderId={}",
                id, outboundStatus, outboundOrderId);
    }

    /**
     * 批量更新订单出库状态
     *
     * @param ids 订单ID列表
     * @param outboundStatus 出库状态
     * @param outboundOrderId 出库单ID（可选）
     * @param outboundTime 出库时间（可选）
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateOutboundStatus(List<Long> ids, OutboundStatus outboundStatus, Long outboundOrderId, LocalDateTime outboundTime) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Assert.notNull(outboundStatus, "出库状态不能为空");

        int affected = baseMapper.batchUpdateOutboundStatus(ids, outboundStatus.name(), outboundOrderId, outboundTime);
        log.info("Batch updated order outbound status, count={}, outboundStatus={}, outboundOrderId={}",
                affected, outboundStatus, outboundOrderId);
    }

    /**
     * 获取待出库订单列表（按平台筛选）
     *
     * @param platform 平台（必填）
     * @return 待出库订单列表
     */
    public List<ErpOrder> getPendingOutboundOrders(String platform) {
        return baseMapper.selectPendingOutboundOrders(platform);
    }

    /**
     * 分页查询待出库订单列表
     *
     * @param pageParam 分页参数
     * @param qo 查询条件（含 articles 字段）
     * @return 分页结果
     */
    public PageResult<ErpOrder> getPendingOutboundOrdersPage(PageParam pageParam, PendingOrderQO qo) {
        return baseMapper.selectPendingOutboundOrdersPage(pageParam, qo);
    }

    /**
     * 占用订单用于出库（乐观锁）
     * <p>
     * 将订单状态从 NONE 更新为 ALLOCATED，使用乐观锁防止并发冲突
     *
     * @param orderIds 订单ID列表
     * @param platform 平台
     */
    @Transactional(rollbackFor = Exception.class)
    public void allocateForOutbound(List<Long> orderIds, String platform) {
        if (orderIds == null || orderIds.isEmpty()) {
            return;
        }

        for (Long orderId : orderIds) {
            ErpOrder order = this.getById(orderId);
            Assert.notNull(order, "订单不存在，订单ID: " + orderId);
            Assert.isTrue(platform != null && platform.equalsIgnoreCase(order.getPlatform()),
					"订单平台不匹配，订单ID: " + orderId);
			Assert.isTrue("SHIPPED".equals(order.getErpStatus()),
					"订单尚未在平台确认，订单ID: " + orderId);
			Assert.isTrue("FBS".equalsIgnoreCase(order.getFulfillmentType()),
					"仅FBS订单可以创建销售出库单，订单ID: " + orderId);
			Assert.isTrue(order.getLocked() == null || order.getLocked() == 0,
					"订单已锁定，订单ID: " + orderId);
            Assert.isTrue("NONE".equals(order.getOutboundStatus()), "订单已被占用，订单ID: " + orderId);

            int affected = baseMapper.allocateForOutboundWithVersion(orderId, order.getVersion());
            Assert.isTrue(affected > 0, "订单占用失败（并发冲突），订单ID: " + orderId);
        }

        log.info("Allocated orders for outbound, orderIds={}, platform={}", orderIds, platform);
    }

    /**
     * 释放订单出库占用
     * <p>
     * 将订单状态从 ALLOCATED 更新为 NONE
     *
     * @param orderIds 订单ID列表
	 * @param outboundOrderId 当前占用所属的销售出库单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseOutboundAllocation(List<Long> orderIds, Long outboundOrderId) {
        if (orderIds == null || orderIds.isEmpty()) {
            return;
        }
		Assert.notNull(outboundOrderId, "出库单ID不能为空");

        int affected = baseMapper.releaseOutboundAllocation(orderIds, outboundOrderId);
        log.info("Released outbound allocation, orderIds={}, outboundOrderId={}, affected={}",
				orderIds, outboundOrderId, affected);
    }

    /** 将已占用订单绑定到销售出库单，仍保持 ALLOCATED，防止重复关联。 */
    @Transactional(rollbackFor = Exception.class)
    public void bindOutboundAllocation(List<Long> orderIds, Long outboundOrderId) {
        if (orderIds == null || orderIds.isEmpty()) {
            return;
        }
        Assert.notNull(outboundOrderId, "出库单ID不能为空");
        for (Long orderId : orderIds) {
            ErpOrder order = this.getById(orderId);
            Assert.notNull(order, "订单不存在，订单ID: " + orderId);
            Assert.isTrue("ALLOCATED".equals(order.getOutboundStatus()), "订单未处于已分配状态，订单ID: " + orderId);
            if (Objects.equals(outboundOrderId, order.getOutboundOrderId())) {
                continue;
            }
            Assert.isNull(order.getOutboundOrderId(), "订单已关联其他出库单，订单ID: " + orderId);
            int affected = baseMapper.bindOutboundWithVersion(orderId, outboundOrderId, order.getVersion());
            Assert.isTrue(affected > 0, "订单绑定出库单失败（并发冲突），订单ID: " + orderId);
        }
        log.info("Bound orders to outbound, orderIds={}, outboundOrderId={}", orderIds, outboundOrderId);
    }

    /** 确认提交前校验订单仍由当前销售出库单占用，不提前完成出库。 */
    public void validateOutboundAllocation(List<Long> orderIds, Long outboundOrderId) {
        if (orderIds == null || orderIds.isEmpty()) {
            return;
        }
        for (Long orderId : orderIds) {
            ErpOrder order = this.getById(orderId);
            Assert.notNull(order, "订单不存在，订单ID: " + orderId);
            Assert.isTrue("ALLOCATED".equals(order.getOutboundStatus())
                            && Objects.equals(outboundOrderId, order.getOutboundOrderId()),
                    "订单未由当前出库单占用，订单ID: " + orderId);
        }
    }

    /** 海外仓签出后，将关联订单原子推进为 COMPLETED。 */
    @Transactional(rollbackFor = Exception.class)
    public void completeOutbound(List<Long> orderIds, Long outboundOrderId) {
        if (orderIds == null || orderIds.isEmpty()) {
            return;
        }
        for (Long orderId : orderIds) {
            ErpOrder order = this.getById(orderId);
            Assert.notNull(order, "订单不存在，订单ID: " + orderId);
            if ("COMPLETED".equals(order.getOutboundStatus())
                    && Objects.equals(outboundOrderId, order.getOutboundOrderId())) {
                continue;
            }
            Assert.isTrue("ALLOCATED".equals(order.getOutboundStatus())
                            && Objects.equals(outboundOrderId, order.getOutboundOrderId()),
                    "订单未由当前出库单占用，无法签出，订单ID: " + orderId);
            int affected = baseMapper.completeOutboundWithVersion(orderId, outboundOrderId, order.getVersion());
            Assert.isTrue(affected > 0, "订单签出确认失败（并发冲突），订单ID: " + orderId);
        }
        log.info("Completed outbound orders after warehouse ship, orderIds={}, outboundOrderId={}",
                orderIds, outboundOrderId);
    }

    /**
     * 批量查询SKU日均销量
     * <p>
     * 统计近30天已完成订单的销量，计算日均值
     *
     * @param skuCodes SKU编码集合
     * @return Map key=skuCode, value=日均销量（向下取整）
     */
    public Map<String, Integer> getDailySalesMap(Set<String> skuCodes) {
        if (CollectionUtils.isEmpty(skuCodes)) {
            return Collections.emptyMap();
        }
        LocalDateTime startTime = LocalDateTime.now().minusDays(ForecastConstants.SALES_STAT_DAYS);
        List<SkuDailySalesVO> results = baseMapper.selectDailySalesBySkuCodes(skuCodes, startTime);

        Map<String, Integer> map = new HashMap<>();
        for (SkuDailySalesVO vo : results) {
            int dailyAvg = (int) (vo.getTotalQuantity() / ForecastConstants.SALES_STAT_DAYS);
            map.put(vo.getSkuCode(), dailyAvg);
        }
        return map;
    }

    /**
     * 查询单个SKU的日均销量
     * <p>
     * 统计近30天已完成订单的销量，计算日均值
     *
     * @param skuCode SKU编码
     * @return 日均销量（向下取整）
     */
    public int getDailySales(String skuCode) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(ForecastConstants.SALES_STAT_DAYS);
        Long totalQty = baseMapper.selectTotalSalesBySkuCode(skuCode, startTime);
        return totalQty != null ? (int) (totalQty / ForecastConstants.SALES_STAT_DAYS) : 0;
    }

    /**
     * 设置订单锁定状态
     *
     * @param id    订单ID
     * @param value 锁定值（1=锁定, 0=解锁）
     * @return 是否更新成功
     */
    public boolean setLocked(Long id, Integer value) {
        if (id == null) {
            return false;
        }
        ErpOrder update = new ErpOrder();
        update.setId(id);
        update.setLocked(value == null ? 0 : value);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 更新订单的 shipmentId（独立事务，避免确认流程整体回滚时 shipmentId 丢失）
     *
     * @param orderId   订单ID
     * @param supplyId  平台 Supply ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateShipmentId(Long orderId, String supplyId) {
        Assert.notNull(orderId, "订单ID不能为空");
        ErpOrder update = new ErpOrder();
        update.setId(orderId);
        update.setShipmentId(supplyId);
        update.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(update);
    }
}
