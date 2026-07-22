package com.erp.admin.order.service;

import com.erp.admin.order.model.entity.ErpOrder;
import org.springframework.stereotype.Service;

/**
 * Platform order lifecycle hook.
 *
 * <p>Marketplace synchronization is an order-domain operation. It must not mutate
 * warehouse inventory or reservation ledgers. Warehouse reservations are created
 * only after a WMS outbound order is confirmed and concrete physical batches are
 * allocated.</p>
 */
@Service
public class OrderReservationService {

    /**
     * Kept as a compatibility hook for all marketplace adapters.
     */
    public void reserveIfNeeded(ErpOrder order) {
        // Intentionally no-op: marketplace orders are demand, not warehouse stock.
    }

    /**
     * Kept as a compatibility hook for all marketplace adapters.
     */
    public void handleStatusChange(ErpOrder order, String oldErpStatus, String newErpStatus) {
        // Intentionally no-op: marketplace status changes do not mutate WMS stock.
    }

}
