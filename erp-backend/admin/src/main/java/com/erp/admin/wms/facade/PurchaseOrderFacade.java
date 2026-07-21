package com.erp.admin.wms.facade;

import com.erp.admin.wms.model.dto.PurchaseOrderDTO;
import com.erp.admin.wms.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 采购单业务编排层
 * <p>
 * 负责采购单写操作的统一入口，持有事务。
 * Service 层专注于单一领域的 CRUD 和状态管理。
 * </p>
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderFacade {

    private final PurchaseOrderService purchaseOrderService;

    /**
     * 创建采购单
     * @param dto 采购单DTO
     * @return 采购单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(PurchaseOrderDTO dto) {
        Long id = purchaseOrderService.saveOrder(dto);
        log.info("Created purchase order via facade, id={}", id);
        return id;
    }

    /**
     * 编辑采购单
     * @param dto 采购单DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(PurchaseOrderDTO dto) {
        purchaseOrderService.updateOrder(dto);
        log.info("Updated purchase order via facade, id={}", dto.getId());
    }

    /**
     * 删除采购单
     * @param id 采购单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        purchaseOrderService.deleteOrder(id);
        log.info("Deleted purchase order via facade, id={}", id);
    }

    /**
     * 确认采购单
     * @param id 采购单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        purchaseOrderService.updateToConfirmed(id);
        log.info("Confirmed purchase order via facade, id={}", id);
    }

    /**
     * 开始生产
     * @param id 采购单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void startProduction(Long id) {
        purchaseOrderService.updateToInProduction(id);
        log.info("Started production for purchase order via facade, id={}", id);
    }

    /**
     * 取消采购单
     * @param id 采购单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        purchaseOrderService.updateToCancelled(id);
        log.info("Cancelled purchase order via facade, id={}", id);
    }

}
