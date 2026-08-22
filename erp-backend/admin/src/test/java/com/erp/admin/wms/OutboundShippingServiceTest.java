package com.erp.admin.wms;

import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.order.service.ErpOrderService;
import com.erp.admin.platform.finance.service.WarehouseBillingService;
import com.erp.admin.wms.mapper.OutboundShippingMapper;
import com.erp.admin.wms.mapper.OutboundPickingMapper;
import com.erp.admin.wms.mapper.SalesOutboundItemMapper;
import com.erp.admin.wms.mapper.SalesOutboundMapper;
import com.erp.admin.wms.mapper.WmsClientBillingRecordMapper;
import com.erp.admin.wms.mapper.WmsOutboundPickAllocationMapper;
import com.erp.admin.wms.mapper.WmsPhysicalInventoryMapper;
import com.erp.admin.wms.model.dto.PackDTO;
import com.erp.admin.wms.model.dto.ShipPackageDTO;
import com.erp.admin.wms.model.dto.ShipDTO;
import com.erp.admin.wms.model.entity.SalesOutboundOrder;
import com.erp.admin.wms.model.entity.SalesOutboundOrderItem;
import com.erp.admin.wms.model.entity.WmsOutboundPickAllocation;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.entity.WmsSalesOutboundPackage;
import com.erp.admin.wms.model.enums.OutboundOrderStatus;
import com.erp.admin.wms.service.OutboundShippingService;
import com.erp.admin.wms.service.WmsInventoryAggregator;
import com.erp.admin.wms.service.WmsPalletService;
import com.erp.admin.wms.service.SalesOutboundPackageService;
import com.erp.admin.wms.service.WarehouseOutboundDocumentService;
import com.erp.admin.wms.service.WmsSortSlotService;
import org.ballcat.common.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 打包签出状态机 + 签出扣库存/释放锁定 测试（Mockito）。
 *
 * @author erp
 */
class OutboundShippingServiceTest {

    private OutboundShippingMapper shippingMapper;
    private SalesOutboundMapper orderMapper;
    private OutboundPickingMapper pickingMapper;
    private SalesOutboundItemMapper itemMapper;
    private WmsPhysicalInventoryMapper physMapper;
    private WmsOutboundPickAllocationMapper allocMapper;
    private WmsClientBillingRecordMapper billingMapper;
    private WmsInventoryAggregator aggregator;
    private com.erp.admin.wms.service.WmsLogisticsProductService logisticsProductService;
    private com.erp.admin.tenant.mapper.SysTenantMapper sysTenantMapper;
    private TenantIdentityService tis;
    private WmsPalletService palletService;
    private ErpOrderService erpOrderService;
    private SalesOutboundPackageService packageService;
    private WarehouseOutboundDocumentService documentService;
    private WarehouseBillingService warehouseBillingService;
    private com.erp.admin.product.service.WarehouseSkuCodeService warehouseSkuCodeService;
    private WmsSortSlotService sortSlotService;
    private OutboundShippingService service;

    @BeforeEach
    void setUp() {
        shippingMapper = mock(OutboundShippingMapper.class);
        orderMapper = mock(SalesOutboundMapper.class);
        pickingMapper = mock(OutboundPickingMapper.class);
        itemMapper = mock(SalesOutboundItemMapper.class);
        physMapper = mock(WmsPhysicalInventoryMapper.class);
        allocMapper = mock(WmsOutboundPickAllocationMapper.class);
        billingMapper = mock(WmsClientBillingRecordMapper.class);
        aggregator = mock(WmsInventoryAggregator.class);
        logisticsProductService = mock(com.erp.admin.wms.service.WmsLogisticsProductService.class);
        sysTenantMapper = mock(com.erp.admin.tenant.mapper.SysTenantMapper.class);
        tis = mock(TenantIdentityService.class);
        palletService = mock(WmsPalletService.class);
        erpOrderService = mock(ErpOrderService.class);
        packageService = mock(SalesOutboundPackageService.class);
        sortSlotService = mock(WmsSortSlotService.class);
        documentService = mock(WarehouseOutboundDocumentService.class);
        warehouseBillingService = mock(WarehouseBillingService.class);
        warehouseSkuCodeService = mock(com.erp.admin.product.service.WarehouseSkuCodeService.class);
        TenantIdentityVO id = mock(TenantIdentityVO.class);
        when(id.getIdentityType()).thenReturn(TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM);
        when(tis.currentIdentity(any())).thenReturn(id);
        service = new OutboundShippingService(shippingMapper, orderMapper, pickingMapper, itemMapper, physMapper, allocMapper,
                billingMapper, aggregator, logisticsProductService, sysTenantMapper, tis, palletService,
                erpOrderService, packageService, documentService, warehouseBillingService, warehouseSkuCodeService,
                sortSlotService);
    }

    private SalesOutboundOrder order(long id, String status) {
        SalesOutboundOrder o = new SalesOutboundOrder();
        o.setId(id);
        o.setErpTenantId(6L);
        o.setWarehouseId(1L);
        o.setOrderStatus(status);
        return o;
    }

    @Test
    void pack_rejects_order_still_picking() {
        when(orderMapper.selectByIdForUpdate(1L)).thenReturn(order(1, OutboundOrderStatus.PICKING.name()));
        PackDTO dto = new PackDTO();
        dto.setOutboundOrderId(1L);
        dto.setPackMode("BY_ORDER");
        assertThatThrownBy(() -> service.pack(dto)).isInstanceOf(BusinessException.class);
    }

    @Test
    void pack_transitions_to_packed() {
        when(orderMapper.selectByIdForUpdate(1L)).thenReturn(order(1, OutboundOrderStatus.PICKED.name()));
        when(orderMapper.updateById(any(SalesOutboundOrder.class))).thenReturn(1);
        PackDTO dto = new PackDTO();
        dto.setOutboundOrderId(1L);
        dto.setPackMode("BY_ORDER");
        service.pack(dto);
        ArgumentCaptor<SalesOutboundOrder> cap = ArgumentCaptor.forClass(SalesOutboundOrder.class);
        verify(orderMapper).updateById(cap.capture());
        assertThat(cap.getValue().getOrderStatus()).isEqualTo(OutboundOrderStatus.PACKED.name());
        assertThat(cap.getValue().getPackMode()).isEqualTo("BY_ORDER");
    }

    @Test
    void ship_rejects_when_not_packed() {
        when(orderMapper.selectById(1L)).thenReturn(order(1, OutboundOrderStatus.PICKING.name()));
        ShipDTO dto = shipDto();
        assertThatThrownBy(() -> service.ship(dto)).isInstanceOf(BusinessException.class);
    }

    @Test
    void ship_deducts_batch_and_releases_reserved_and_marks_shipped() {
        SalesOutboundOrder outbound = order(1, OutboundOrderStatus.PACKED.name());
        outbound.setSourceType("CUSTOM");
        when(packageService.allPacked(1L)).thenReturn(true);
        when(orderMapper.selectById(1L)).thenReturn(outbound);
        SalesOutboundOrderItem item = new SalesOutboundOrderItem();
        item.setErpOrderId(88L);
        when(itemMapper.selectByOutboundOrderId(1L)).thenReturn(Collections.singletonList(item));
        WmsOutboundPickAllocation a = new WmsOutboundPickAllocation();
        a.setPhysicalInventoryId(100L);
        a.setSkuCode("SKU1");
        a.setTakeQty(4);
        when(allocMapper.selectByOutboundOrderId(1L)).thenReturn(Collections.singletonList(a));
        WmsPhysicalInventory batch = new WmsPhysicalInventory();
        batch.setId(100L);
        batch.setQuantity(10);
        batch.setReservedQty(4);
        when(physMapper.selectById(100L)).thenReturn(batch);
        when(shippingMapper.selectChannels()).thenReturn(Collections.emptyList());
        // BUG#4：签出入口状态 CAS 抢占成功(1) + 批次乐观锁扣减命中(1)
        when(orderMapper.casOrderStatus(anyLong(), anyString(), anyString())).thenReturn(1);
        when(physMapper.updateById(any(WmsPhysicalInventory.class))).thenReturn(1);

        ShipDTO dto = shipDto();
        service.ship(dto);

        // 批次：quantity 10-4=6，reserved 4-4=0
        ArgumentCaptor<WmsPhysicalInventory> bcap = ArgumentCaptor.forClass(WmsPhysicalInventory.class);
        verify(physMapper).updateById(bcap.capture());
        assertThat(bcap.getValue().getQuantity()).isEqualTo(6);
        assertThat(bcap.getValue().getReservedQty()).isEqualTo(0);
        // 聚合刷新受影响 SKU
        verify(aggregator).refreshSnapshot(eq(0L), eq(6L), eq(1L), eq("SKU1"));
        verify(erpOrderService).completeOutbound(Collections.singletonList(88L), 1L);
        // 无物流产品 → 不计费
        verify(billingMapper, never()).insert(any());
        // 订单转 SHIPPED
        ArgumentCaptor<SalesOutboundOrder> ocap = ArgumentCaptor.forClass(SalesOutboundOrder.class);
        verify(orderMapper).updateById(ocap.capture());
        assertThat(ocap.getValue().getOrderStatus()).isEqualTo(OutboundOrderStatus.SHIPPED.name());
        assertThat(ocap.getValue().getTrackingNo()).isEqualTo("TRK123");
    }

    @Test
    void ship_with_product_bills_parent_operator_at_unit_price() {
        SalesOutboundOrder o = order(1, OutboundOrderStatus.PACKED.name());
        o.setLogisticsProductId(9L);
        o.setOutboundDate(java.time.LocalDate.of(2026, 7, 1));
        when(orderMapper.selectById(1L)).thenReturn(o);
        WmsOutboundPickAllocation a = new WmsOutboundPickAllocation();
        a.setPhysicalInventoryId(100L);
        a.setSkuCode("SKU1");
        a.setTakeQty(2);
        when(allocMapper.selectByOutboundOrderId(1L)).thenReturn(Collections.singletonList(a));
        WmsPhysicalInventory batch = new WmsPhysicalInventory();
        batch.setId(100L);
        batch.setQuantity(10);
        batch.setReservedQty(2);
        when(physMapper.selectById(100L)).thenReturn(batch);
        when(shippingMapper.selectChannels()).thenReturn(Collections.emptyList());
        // 产品单价 15.50
        com.erp.admin.wms.model.entity.WmsLogisticsProduct product =
                new com.erp.admin.wms.model.entity.WmsLogisticsProduct();
        product.setId(9L);
        product.setUnitPrice(new BigDecimal("15.50"));
        when(logisticsProductService.getEnabledById(9L)).thenReturn(product);
        // 货主6 的父服务商 = 5
        com.erp.admin.tenant.model.entity.SysTenant owner = new com.erp.admin.tenant.model.entity.SysTenant();
        owner.setId(6L);
        owner.setParentWmsTenantId(5L);
        when(sysTenantMapper.selectById(6L)).thenReturn(owner);
        when(billingMapper.selectByBizId("SHIP:1")).thenReturn(null);
        // BUG#4：状态 CAS 抢占成功 + 批次乐观锁扣减命中
        when(orderMapper.casOrderStatus(anyLong(), anyString(), anyString())).thenReturn(1);
        when(physMapper.updateById(any(WmsPhysicalInventory.class))).thenReturn(1);

        com.erp.admin.wms.model.vo.ShipResultVO result = service.ship(shipDto());

        ArgumentCaptor<com.erp.admin.wms.model.entity.WmsClientBillingRecord> cap =
                ArgumentCaptor.forClass(com.erp.admin.wms.model.entity.WmsClientBillingRecord.class);
        verify(billingMapper).insert(cap.capture());
        assertThat(cap.getValue().getWmsTenantId()).isEqualTo(5L);       // 归属父服务商
        assertThat(cap.getValue().getAmount()).isEqualByComparingTo("15.50");
        assertThat(cap.getValue().getBillMonth()).isEqualTo("2026-07");
        assertThat(cap.getValue().getBizId()).isEqualTo("SHIP:1");
        assertThat(result.getShippingFee()).isEqualByComparingTo("15.50");
    }

    @Test
    void ship_is_idempotent_guarded() {
        when(orderMapper.selectById(1L)).thenReturn(order(1, OutboundOrderStatus.SHIPPED.name()));
        assertThatThrownBy(() -> service.ship(shipDto())).isInstanceOf(BusinessException.class);
        verify(physMapper, never()).updateById(any());
    }

    @Test
    void shipPackage_allows_current_packed_package_while_other_packages_are_not_packed() {
        SalesOutboundOrder outbound = order(1, OutboundOrderStatus.PICKED.name());
        outbound.setSourceType("SALES");
        when(orderMapper.selectByIdForUpdate(1L)).thenReturn(outbound);
        WmsSalesOutboundPackage pack = packageEntity(11L, 1L, 88L);
        when(packageService.getForUpdate(11L)).thenReturn(pack);

        SalesOutboundOrderItem packageItem = new SalesOutboundOrderItem();
        packageItem.setErpOrderId(88L);
        packageItem.setSkuCode("SKU1");
        packageItem.setQuantity(2);
        when(itemMapper.selectPackageItemsForUpdate(1L, 88L))
                .thenReturn(Collections.singletonList(packageItem));

        WmsOutboundPickAllocation allocation = new WmsOutboundPickAllocation();
        allocation.setId(101L);
        allocation.setOutboundOrderId(1L);
        allocation.setPhysicalInventoryId(100L);
        allocation.setSkuCode("SKU1");
        allocation.setTakeQty(5);
        allocation.setShippedQty(0);
        when(allocMapper.selectByOutboundOrderIdForUpdate(1L))
                .thenReturn(Collections.singletonList(allocation));
        when(allocMapper.updateById(any(WmsOutboundPickAllocation.class))).thenReturn(1);

        WmsPhysicalInventory batch = new WmsPhysicalInventory();
        batch.setId(100L);
        batch.setQuantity(10);
        batch.setReservedQty(5);
        when(physMapper.selectById(100L)).thenReturn(batch);
        when(physMapper.updateById(any(WmsPhysicalInventory.class))).thenReturn(1);
        when(packageService.allShipped(1L)).thenReturn(false);

        service.shipPackage(shipPackageDto(1L, 11L, "OZON_A", "PKG-TRK-1"), 99L);

        verify(documentService).assertPackageReadyForShip(outbound, pack);
        assertThat(batch.getQuantity()).isEqualTo(8);
        assertThat(batch.getReservedQty()).isEqualTo(3);
        assertThat(allocation.getShippedQty()).isEqualTo(2);
        verify(packageService).markShipped(eq(11L), eq("OZON_A"), anyString(),
                eq("PKG-TRK-1"), eq(new BigDecimal("1.5")), eq(99L), any());
        verify(orderMapper, never()).casOrderStatus(anyLong(), anyString(), anyString());
        verify(erpOrderService).completeOutboundForWarehouse(Collections.singletonList(88L), 1L, 6L);
        verify(sortSlotService).releaseByPackage(11L);
    }

    @Test
    void shipPackage_last_package_completes_master_order() {
        SalesOutboundOrder outbound = order(1, OutboundOrderStatus.PACKED.name());
        outbound.setSourceType("SALES");
        when(orderMapper.selectByIdForUpdate(1L)).thenReturn(outbound);
        WmsSalesOutboundPackage pack = packageEntity(12L, 1L, 89L);
        when(packageService.getForUpdate(12L)).thenReturn(pack);

        SalesOutboundOrderItem packageItem = new SalesOutboundOrderItem();
        packageItem.setErpOrderId(89L);
        packageItem.setSkuCode("SKU2");
        packageItem.setQuantity(1);
        when(itemMapper.selectPackageItemsForUpdate(1L, 89L))
                .thenReturn(Collections.singletonList(packageItem));

        WmsOutboundPickAllocation allocation = new WmsOutboundPickAllocation();
        allocation.setId(102L);
        allocation.setOutboundOrderId(1L);
        allocation.setPhysicalInventoryId(200L);
        allocation.setSkuCode("SKU2");
        allocation.setTakeQty(1);
        allocation.setShippedQty(0);
        when(allocMapper.selectByOutboundOrderIdForUpdate(1L))
                .thenReturn(Collections.singletonList(allocation));
        when(allocMapper.selectByOutboundOrderId(1L))
                .thenReturn(Collections.singletonList(allocation));
        when(allocMapper.updateById(any(WmsOutboundPickAllocation.class))).thenReturn(1);

        WmsPhysicalInventory batch = new WmsPhysicalInventory();
        batch.setId(200L);
        batch.setQuantity(1);
        batch.setReservedQty(1);
        when(physMapper.selectById(200L)).thenReturn(batch);
        when(physMapper.updateById(any(WmsPhysicalInventory.class))).thenReturn(1);
        when(packageService.allShipped(1L)).thenReturn(true);
        when(orderMapper.casOrderStatus(1L, OutboundOrderStatus.PACKED.name(),
                OutboundOrderStatus.SHIPPED.name())).thenReturn(1);

        service.shipPackage(shipPackageDto(1L, 12L, "YANDEX", "PKG-TRK-2"), 99L);

        verify(orderMapper).casOrderStatus(1L, OutboundOrderStatus.PACKED.name(),
                OutboundOrderStatus.SHIPPED.name());
        verify(packageService).markShipped(eq(12L), eq("YANDEX"), anyString(),
                eq("PKG-TRK-2"), eq(new BigDecimal("1.5")), eq(99L), any());
        assertThat(batch.getQuantity()).isZero();
        assertThat(allocation.getShippedQty()).isEqualTo(1);
    }

    private ShipDTO shipDto() {
        ShipDTO dto = new ShipDTO();
        dto.setOutboundOrderId(1L);
        dto.setChannel("AUTO");
        dto.setTrackingNo("TRK123");
        dto.setWeight(new BigDecimal("1.5"));
        return dto;
    }

    private ShipPackageDTO shipPackageDto(long outboundId, long packageId, String channel, String trackingNo) {
        ShipPackageDTO dto = new ShipPackageDTO();
        dto.setOutboundOrderId(outboundId);
        dto.setPackageId(packageId);
        dto.setChannel(channel);
        dto.setTrackingNo(trackingNo);
        dto.setWeight(new BigDecimal("1.5"));
        return dto;
    }

    private WmsSalesOutboundPackage packageEntity(long id, long outboundId, long erpOrderId) {
        WmsSalesOutboundPackage pack = new WmsSalesOutboundPackage();
        pack.setId(id);
        pack.setOutboundOrderId(outboundId);
        pack.setErpOrderId(erpOrderId);
        pack.setPackStatus(SalesOutboundPackageService.PACK_PACKED);
        pack.setShipStatus(SalesOutboundPackageService.SHIP_PENDING);
        return pack;
    }

}
