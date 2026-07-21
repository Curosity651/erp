package com.erp.admin.wms.facade;

import com.erp.admin.wms.model.dto.CustomOutboundDTO;
import com.erp.admin.wms.model.entity.SalesOutboundOrder;
import com.erp.admin.wms.model.entity.SalesOutboundOrderItem;
import com.erp.admin.wms.model.enums.OutboundOrderStatus;
import com.erp.admin.wms.model.vo.StockShortageVO;
import com.erp.admin.wms.service.CustomOutboundService;
import com.erp.admin.wms.service.WmsPhysicalInventoryService;
import com.erp.admin.wms.service.OutboundPickingService;
import com.erp.admin.wms.service.SalesOutboundItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 自定义出库单业务编排层
 * <p>
 * 负责跨服务的业务流程编排，持有事务。与销售出库的差异：
 * 1) 不挂平台订单，无订单占用/释放；
 * 2) 无区域预占（区域预占来自平台订单生命周期），提交时仅做「仓库可售校验 + 仓库可用扣减」预占；
 * 3) 提交（DRAFT→CONFIRMED）后进入海外仓平台作业台（下架→打包→签出），与销售出库单同链路处理。
 * </p>
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOutboundFacade {

    private final CustomOutboundService customOutboundService;
    private final SalesOutboundItemService salesOutboundItemService;
    private final WmsPhysicalInventoryService physicalInventoryService;
    private final OutboundPickingService outboundPickingService;

    /**
     * 创建出库单草稿（不校验库存、不预占，提交时统一处理）
     *
     * @param dto 出库单DTO
     * @return 出库单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(CustomOutboundDTO dto) {
        Long orderId = customOutboundService.saveOrder(dto);
        log.info("Created custom outbound order via facade, id={}, customType={}", orderId, dto.getCustomType());
        return orderId;
    }

    /**
     * 更新出库单草稿
     *
     * @param dto 出库单DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(CustomOutboundDTO dto) {
        customOutboundService.updateOrder(dto);
        log.info("Updated custom outbound order via facade, id={}", dto.getId());
    }

    /**
     * 提交出库单（DRAFT→CONFIRMED）
     * 1. 按可售库存硬校验，不足则返回缺口明细（不提交）
     * 2. 校验通过执行库存过账（仓库可用扣减 = 预占，防止与销售出库/其他单据抢货）
     * 3. 更新出库单状态为 CONFIRMED，进入平台作业台待下架
     *
     * @param id 出库单ID
     * @return 库存不足明细列表（空列表表示成功）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<StockShortageVO> submit(Long id) {
        SalesOutboundOrder order = customOutboundService.getByIdOrThrow(id);
        Assert.isTrue(OutboundOrderStatus.DRAFT.name().equals(order.getOrderStatus()),
                "只有草稿状态的出库单可以提交");

        List<SalesOutboundOrderItem> items = salesOutboundItemService.getByOutboundOrderId(id);
        Assert.notEmpty(items, "出库明细不能为空");

        // 1. 按可售库存硬校验（快照预检，快速失败）
        List<StockShortageVO> shortages = checkStockSufficiency(order, items);
        if (!shortages.isEmpty()) {
            return shortages;
        }

        // 2. 批次预留（方案A）：提交时就 FIFO 锁批次 + 建拣货分配 + 刷新快照，可用当场下降。
        //    缺货则返回明细、事务回滚、单据保持 DRAFT。（自定义出库无区域/无 ERP 订单）
        List<StockShortageVO> reserveShortages = outboundPickingService.reserveForOrder(order, items);
        if (!reserveShortages.isEmpty()) {
            return reserveShortages;
        }

        // 3. 更新出库单状态（预留不产生过账单，postingId 传 null）
        customOutboundService.updateToConfirmed(id, null);

        log.info("Submitted custom outbound order via facade, id={}", id);
        return Collections.emptyList();
    }

    /**
     * 取消出库单
     * 1. 草稿态：直接取消
     * 2. 已确认（未开始下架）：反向过账回补可用库存（释放预占）后取消
     * 已进入下架/打包/签出流程的单据不可取消。
     *
     * @param id 出库单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        SalesOutboundOrder order = customOutboundService.getByIdOrThrow(id);
        String status = order.getOrderStatus();
        Assert.isTrue(OutboundOrderStatus.DRAFT.name().equals(status)
                        || OutboundOrderStatus.CONFIRMED.name().equals(status),
                "仅草稿或已提交且未开始下架的出库单可以取消");

        // 已确认单：释放批次预留（可用回补），与提交时的批次预留对称（方案A）
        if (OutboundOrderStatus.CONFIRMED.name().equals(status)) {
            outboundPickingService.releaseForOrder(order);
        }

        customOutboundService.updateToCancelled(id);
        log.info("Cancelled custom outbound order via facade, id={}, prevStatus={}", id, status);
    }

    /**
     * 删除出库单（仅草稿）
     *
     * @param ids 出库单ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            SalesOutboundOrder order = customOutboundService.getByIdOrThrow(id);
            Assert.isTrue(OutboundOrderStatus.DRAFT.name().equals(order.getOrderStatus()),
                    "只有草稿状态的出库单可以删除");
            customOutboundService.deleteOrder(id);
            log.info("Deleted custom outbound order via facade, id={}", id);
        }
    }

    /**
     * 校验可售库存是否充足（仅仓库可用维度，无区域预占）
     */
    private List<StockShortageVO> checkStockSufficiency(SalesOutboundOrder order, List<SalesOutboundOrderItem> items) {
        // 按 SKU 汇总需求数量
        Map<String, Integer> requiredMap = items.stream()
                .collect(Collectors.groupingBy(
                        SalesOutboundOrderItem::getSkuCode,
                        Collectors.summingInt(SalesOutboundOrderItem::getQuantity)
                ));

        // 查询仓库库存（按货主隔离）
        List<String> skuCodes = new ArrayList<>(requiredMap.keySet());
        Map<String, Integer> stockMap = physicalInventoryService
                .getAllocatableQuantityMap(order.getErpTenantId(), order.getWarehouseId(), skuCodes);

        List<StockShortageVO> shortages = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : requiredMap.entrySet()) {
            String skuCode = entry.getKey();
            int required = entry.getValue();
            int available = stockMap.getOrDefault(skuCode, 0);
            if (available < required) {
                StockShortageVO shortage = new StockShortageVO();
                shortage.setSkuCode(skuCode);
                shortage.setSkuName(skuCode);
                shortage.setRequiredQty(required);
                shortage.setAvailableQty(available);
                shortage.setShortage(required - available);
                shortages.add(shortage);
            }
        }
        return shortages;
    }

}
