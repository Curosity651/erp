package com.erp.admin.wms;

import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.order.service.ErpOrderService;
import com.erp.admin.wms.mapper.OutboundShippingMapper;
import com.erp.admin.wms.mapper.SalesOutboundItemMapper;
import com.erp.admin.wms.mapper.SalesOutboundMapper;
import com.erp.admin.wms.mapper.WmsClientBillingRecordMapper;
import com.erp.admin.wms.mapper.WmsOutboundPickAllocationMapper;
import com.erp.admin.wms.mapper.WmsPhysicalInventoryMapper;
import com.erp.admin.wms.model.dto.PackDTO;
import com.erp.admin.wms.model.dto.ShipDTO;
import com.erp.admin.wms.model.entity.SalesOutboundOrder;
import com.erp.admin.wms.model.entity.SalesOutboundOrderItem;
import com.erp.admin.wms.model.entity.WmsOutboundPickAllocation;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.enums.OutboundOrderStatus;
import com.erp.admin.wms.service.OutboundShippingService;
import com.erp.admin.wms.service.WmsInventoryAggregator;
import com.erp.admin.wms.service.WmsPalletService;
import com.erp.admin.wms.service.SalesOutboundPackageService;
import com.erp.admin.wms.service.WarehouseOutboundDocumentService;
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
    private OutboundShippingService service;

    @BeforeEach
    void setUp() {
        shippingMapper = mock(OutboundShippingMapper.class);
        orderMapper = mock(SalesOutboundMapper.class);
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
        documentService = mock(WarehouseOutboundDocumentService.class);
        TenantIdentityVO id = mock(TenantIdentityVO.class);
        when(id.getIdentityType()).thenReturn(TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM);
        when(tis.currentIdentity(any())).thenReturn(id);
        service = new OutboundShippingService(shippingMapper, orderMapper, itemMapper, physMapper, allocMapper,
                billingMapper, aggregator, logisticsProductService, sysTenantMapper, tis, palletService,
                erpOrderService, packageService, documentService);
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
        outbound.setSourceType("SALES");
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

    private ShipDTO shipDto() {
        ShipDTO dto = new ShipDTO();
        dto.setOutboundOrderId(1L);
        dto.setChannel("AUTO");
        dto.setTrackingNo("TRK123");
        dto.setWeight(new BigDecimal("1.5"));
        return dto;
    }

}
