package com.erp.admin.wms;

import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.entity.SalesOutboundOrder;
import com.erp.admin.wms.model.entity.WmsOutboundPickTaskLine;
import com.erp.admin.wms.service.OutboundPickingService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void sales_order_count_is_distinct_from_outbound_document_count() {
        SalesOutboundOrder first = new SalesOutboundOrder();
        first.setOrderCount(4);
        SalesOutboundOrder second = new SalesOutboundOrder();
        second.setOrderCount(5);

        assertThat(Arrays.asList(first, second)).hasSize(2);
        assertThat(OutboundPickingService.salesOrderCount(Arrays.asList(first, second))).isEqualTo(9);
    }

    @Test
    void pick_tasks_are_split_by_final_package_capacity() {
        SalesOutboundOrder first = new SalesOutboundOrder();
        first.setOrderCount(4);
        SalesOutboundOrder second = new SalesOutboundOrder();
        second.setOrderCount(5);

        List<List<SalesOutboundOrder>> groups = OutboundPickingService
                .splitByPackageCapacity(Arrays.asList(first, second), 6);

        assertThat(groups).hasSize(2);
        assertThat(OutboundPickingService.salesOrderCount(groups.get(0))).isEqualTo(4);
        assertThat(OutboundPickingService.salesOrderCount(groups.get(1))).isEqualTo(5);
    }

    @Test
    void one_outbound_document_is_never_split_between_pick_tasks() {
        SalesOutboundOrder order = new SalesOutboundOrder();
        order.setOrderCount(9);

        List<List<SalesOutboundOrder>> groups = OutboundPickingService
                .splitByPackageCapacity(Collections.singletonList(order), 6);

        assertThat(groups).hasSize(1);
        assertThat(groups.get(0)).containsExactly(order);
    }

    @Test
    void direct_order_packing_is_default_and_sort_slots_are_explicit() {
        assertThat(OutboundPickingService.requiresSlotSorting(null, "SALES", 3)).isFalse();
        assertThat(OutboundPickingService.requiresSlotSorting(false, "SALES", 3)).isFalse();
        assertThat(OutboundPickingService.requiresSlotSorting(true, "SALES", 3)).isTrue();
        assertThat(OutboundPickingService.requiresSlotSorting(true, "SALES", 1)).isFalse();
        assertThat(OutboundPickingService.requiresSlotSorting(true, "CUSTOM", 3)).isFalse();
    }

    @Test
    void task_cannot_complete_until_every_line_is_actually_picked() {
        WmsOutboundPickTaskLine complete = new WmsOutboundPickTaskLine();
        complete.setPlannedQty(5);
        complete.setPickedQty(5);
        complete.setLineStatus("COMPLETED");
        WmsOutboundPickTaskLine incomplete = new WmsOutboundPickTaskLine();
        incomplete.setPlannedQty(3);
        incomplete.setPickedQty(2);
        incomplete.setLineStatus("IN_PROGRESS");

        assertThat(OutboundPickingService.isTaskCompletable(Arrays.asList(complete, incomplete))).isFalse();

        incomplete.setPickedQty(3);
        incomplete.setLineStatus("COMPLETED");
        assertThat(OutboundPickingService.isTaskCompletable(Arrays.asList(complete, incomplete))).isTrue();

        incomplete.setLineStatus("EXCEPTION");
        assertThat(OutboundPickingService.isTaskCompletable(Arrays.asList(complete, incomplete))).isFalse();
    }

    @Test
    void piece_pick_rejects_pallet_only_scan() {
        assertThatThrownBy(() -> OutboundPickingService.validatePickScan(
                "PIECE", "A10-01", "A10-01-L1-P01", "PLT001",
                "A10-01-L1-P01", true, false, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("拆零");
    }

    @Test
    void piece_pick_requires_exact_slot_instead_of_parent_location() {
        assertThatThrownBy(() -> OutboundPickingService.validatePickScan(
                "PIECE", "A10-01", "A10-01-L1-P01", "PLT001",
                "A10-01", false, true, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("托位");
    }

    @Test
    void whole_pallet_scan_uses_all_remaining_quantity() {
        OutboundPickingService.validatePickScan(
                "WHOLE_PALLET", "A10-01", "A10-01-L1-P01", "PLT001",
                "A10-01-L1-P01", true, false, false, null);

        assertThat(OutboundPickingService.resolveScanQuantity("WHOLE_PALLET", 1, 18)).isEqualTo(18);
        assertThat(OutboundPickingService.resolveScanQuantity("PIECE", 2, 18)).isEqualTo(2);
    }

    @Test
    void manual_pick_requires_reason() {
        assertThatThrownBy(() -> OutboundPickingService.validatePickScan(
                "PIECE", "A10-01", "A10-01-L1-P01", "PLT001",
                null, false, false, true, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("原因");
    }

    @Test
    void partially_picked_task_requires_return_before_short_close() {
        WmsOutboundPickTaskLine untouched = new WmsOutboundPickTaskLine();
        untouched.setPickedQty(0);
        WmsOutboundPickTaskLine picked = new WmsOutboundPickTaskLine();
        picked.setPickedQty(2);

        assertThat(OutboundPickingService.requiresReturnBeforeShortClose(
                Arrays.asList(untouched, picked))).isTrue();
        picked.setPickedQty(0);
        assertThat(OutboundPickingService.requiresReturnBeforeShortClose(
                Arrays.asList(untouched, picked))).isFalse();
    }

}
