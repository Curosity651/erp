package com.erp.admin.wms;

import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.service.OutboundPickingService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 下架 FIFO 纯逻辑测试：分配次序、缺口、跳过占满批次、可用量、状态映射。
 *
 * @author erp
 */
class OutboundPickingServiceTest {

    private WmsPhysicalInventory batch(long id, String date, int order, int qty, int reserved) {
        WmsPhysicalInventory b = new WmsPhysicalInventory();
        b.setId(id);
        b.setInboundDate(LocalDate.parse(date));
        b.setPickOrder(order);
        b.setQuantity(qty);
        b.setReservedQty(reserved);
        return b;
    }

    @Test
    void fifo_allocates_in_order_until_satisfied() {
        List<WmsPhysicalInventory> batches = Arrays.asList(
                batch(1, "2026-06-01", 1, 5, 0),
                batch(2, "2026-06-02", 1, 10, 0));
        OutboundPickingService.AllocPlan plan = OutboundPickingService.planAllocation(batches, 8);
        assertThat(plan.shortage).isZero();
        assertThat(plan.takes).hasSize(2);
        assertThat(plan.takes.get(0).take).isEqualTo(5);  // 先取最早批次全部
        assertThat(plan.takes.get(1).take).isEqualTo(3);  // 次批次补 3
    }

    @Test
    void fifo_reports_shortage_when_insufficient() {
        List<WmsPhysicalInventory> batches = Collections.singletonList(batch(1, "2026-06-01", 1, 4, 1));
        OutboundPickingService.AllocPlan plan = OutboundPickingService.planAllocation(batches, 10);
        // 可用 = 4 - 1 = 3；缺口 7
        assertThat(plan.shortage).isEqualTo(7);
        assertThat(plan.takes).hasSize(1);
        assertThat(plan.takes.get(0).take).isEqualTo(3);
    }

    @Test
    void fifo_skips_fully_reserved_batch() {
        List<WmsPhysicalInventory> batches = Arrays.asList(
                batch(1, "2026-06-01", 1, 5, 5),   // 占满，跳过
                batch(2, "2026-06-02", 1, 6, 0));
        OutboundPickingService.AllocPlan plan = OutboundPickingService.planAllocation(batches, 4);
        assertThat(plan.shortage).isZero();
        assertThat(plan.takes).hasSize(1);
        assertThat(plan.takes.get(0).batch.getId()).isEqualTo(2L);
        assertThat(plan.takes.get(0).take).isEqualTo(4);
    }

    @Test
    void available_qty_sums_positive_only() {
        List<WmsPhysicalInventory> batches = Arrays.asList(
                batch(1, "2026-06-01", 1, 5, 5),   // 0
                batch(2, "2026-06-02", 1, 6, 2),   // 4
                batch(3, "2026-06-03", 1, 3, 0));  // 3
        assertThat(OutboundPickingService.availableQty(batches)).isEqualTo(7);
    }

    @Test
    void status_mapping_confirmed_is_pending() {
        assertThat(OutboundPickingService.toViewStatus("CONFIRMED")).isEqualTo("PENDING");
        assertThat(OutboundPickingService.toViewStatus("PICKING")).isEqualTo("PICKING");
        assertThat(OutboundPickingService.toDbStatus("PENDING")).isEqualTo("CONFIRMED");
        assertThat(OutboundPickingService.toDbStatus("BACKORDER")).isEqualTo("BACKORDER");
    }

}
