package com.erp.admin.wms;

import com.erp.admin.order.mapper.ErpOrderItemMapper;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.system.service.SysFileService;
import com.erp.admin.product.service.WarehouseSkuCodeService;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.service.WmsRackAssignmentService;
import com.erp.admin.wms.mapper.ReturnInboundMapper;
import com.erp.admin.wms.mapper.ReturnQcMapper;
import com.erp.admin.wms.mapper.WmsReturnQcItemMapper;
import com.erp.admin.wms.mapper.WmsSkuLookupMapper;
import com.erp.admin.wms.model.dto.PutawayDTO;
import com.erp.admin.wms.model.dto.LocationInventoryKey;
import com.erp.admin.wms.model.vo.LocationCapacityVO;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.wms.model.dto.ReturnQcDTO;
import com.erp.admin.wms.model.dto.ReturnReceiveDTO;
import com.erp.admin.wms.model.entity.ReturnInboundOrder;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsReturnQcItem;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.model.enums.ReturnQcStatus;
import com.erp.admin.wms.model.vo.PalletSummaryVO;
import com.erp.admin.wms.service.ReturnQcService;
import com.erp.admin.wms.service.WarehouseService;
import com.erp.admin.wms.service.WmsLocationService;
import com.erp.admin.wms.service.WmsPalletService;
import com.erp.admin.wms.service.WmsPhysicalInventoryService;
import com.erp.admin.wms.service.WmsZoneService;
import org.ballcat.common.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ballcat.security.core.PrincipalAttributeAccessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

/**
 * 退货质检状态机 + 良品/残次品拆分批次测试（Mockito）。
 *
 * @author erp
 */
class ReturnQcServiceTest {

    private ReturnQcMapper returnQcMapper;
    private ReturnInboundMapper returnInboundMapper;
    private WmsReturnQcItemMapper itemMapper;
    private WmsPhysicalInventoryService physicalInventoryService;
    private com.erp.admin.wms.service.LocationInventoryService locationInventoryService;
    private com.erp.admin.wms.service.LocationCapacityService locationCapacityService;
    private com.erp.admin.wms.mapper.WmsLocationMapper wmsLocationMapper;
    private com.erp.admin.product.mapper.SkuMapper skuMapper;
    private WmsLocationService locationService;
    private WmsZoneService zoneService;
    private TenantIdentityService tis;
    private ErpOrderItemMapper erpOrderItemMapper;
    private ErpOrderMapper erpOrderMapper;
    private SysTenantMapper sysTenantMapper;
    private WmsRackAssignmentService wmsRackAssignmentService;
    private WarehouseService warehouseService;
    private WmsPalletService palletService;
    private WmsSkuLookupMapper skuLookupMapper;
    private SysFileService sysFileService;
    private WarehouseSkuCodeService warehouseSkuCodeService;
    private PrincipalAttributeAccessor principalAttributeAccessor;
    private ReturnQcService service;

    @BeforeEach
    void setUp() {
        returnQcMapper = mock(ReturnQcMapper.class);
        returnInboundMapper = mock(ReturnInboundMapper.class);
        itemMapper = mock(WmsReturnQcItemMapper.class);
        physicalInventoryService = mock(WmsPhysicalInventoryService.class);
        locationInventoryService = mock(com.erp.admin.wms.service.LocationInventoryService.class);
        locationCapacityService = mock(com.erp.admin.wms.service.LocationCapacityService.class);
        wmsLocationMapper = mock(com.erp.admin.wms.mapper.WmsLocationMapper.class);
        skuMapper = mock(com.erp.admin.product.mapper.SkuMapper.class);
        locationService = mock(WmsLocationService.class);
        zoneService = mock(WmsZoneService.class);
        tis = mock(TenantIdentityService.class);
        erpOrderItemMapper = mock(ErpOrderItemMapper.class);
        erpOrderMapper = mock(ErpOrderMapper.class);
        sysTenantMapper = mock(SysTenantMapper.class);
        wmsRackAssignmentService = mock(WmsRackAssignmentService.class);
        warehouseService = mock(WarehouseService.class);
        palletService = mock(WmsPalletService.class);
        skuLookupMapper = mock(WmsSkuLookupMapper.class);
        sysFileService = mock(SysFileService.class);
        warehouseSkuCodeService = mock(WarehouseSkuCodeService.class);
        principalAttributeAccessor = mock(PrincipalAttributeAccessor.class);
        when(principalAttributeAccessor.getUserId()).thenReturn(88L);
        TenantIdentityVO id = mock(TenantIdentityVO.class);
        when(id.getIdentityType()).thenReturn(TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM);
        when(tis.currentIdentity(any())).thenReturn(id);
        // 货主→父服务商→有效租用货架排：测试用库位在 A1/D1 排
        SysTenant owner = new SysTenant();
        owner.setParentWmsTenantId(5L);
        when(sysTenantMapper.selectById(any())).thenReturn(owner);
        when(wmsRackAssignmentService.activeRackNos(any(), any()))
                .thenReturn(new java.util.HashSet<>(Arrays.asList("A1", "D1")));
        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setWarehouseType("OWN");
        warehouse.setStatus(1);
        when(warehouseService.getById(1L)).thenReturn(warehouse);
        when(wmsLocationMapper.selectLogicalByIdForUpdate(any())).thenAnswer(invocation -> {
            Long idValue = invocation.getArgument(0);
            return location(idValue != null && idValue == 2L ? "D1-01" : "A1-01",
                    idValue != null && idValue == 2L ? 9L : 7L);
        });
        when(zoneService.getById(any())).thenAnswer(invocation -> {
            Long idValue = invocation.getArgument(0);
            return zone(idValue == 9L ? 9L : 7L, idValue == 9L ? "DEFECTIVE" : "RETURN");
        });
        Sku sku = new Sku();
        sku.setSkuCode("SKU1");
        sku.setOuterLengthMm(100);
        sku.setOuterWidthMm(100);
        sku.setOuterHeightMm(100);
        sku.setOuterGrossWeightG(1000);
        when(skuMapper.selectBySkuCode("SKU1")).thenReturn(sku);
        LocationCapacityVO capacity = new LocationCapacityVO();
        capacity.setVolumeAllowed(true);
        capacity.setWeightAllowed(true);
        capacity.setSkuKindsAllowed(true);
        when(locationCapacityService.evaluate(any(), any())).thenReturn(capacity);
        service = new ReturnQcService(returnQcMapper, returnInboundMapper, itemMapper, physicalInventoryService,
                locationInventoryService, locationCapacityService, wmsLocationMapper, skuMapper,
                locationService, zoneService, tis, erpOrderItemMapper, erpOrderMapper,
                sysTenantMapper, wmsRackAssignmentService, warehouseService, palletService,
                skuLookupMapper, sysFileService, warehouseSkuCodeService, principalAttributeAccessor);
    }

    private ReturnInboundOrder order(String status) {
        ReturnInboundOrder o = new ReturnInboundOrder();
        o.setId(1L);
        o.setErpTenantId(6L);
        o.setWarehouseId(1L);
        o.setSkuCode("SKU1");
        o.setTotalQuantity(5);
        o.setReturnStatus(status);
        return o;
    }

    private WmsZone zone(long id, String type) {
        WmsZone z = new WmsZone();
        z.setId(id);
        z.setZoneType(type);
        return z;
    }

    private WmsLocation location(String code, long zoneId) {
        WmsLocation l = new WmsLocation();
        l.setId(code.startsWith("D") ? 2L : 1L);
        l.setLocationCode(code);
        l.setZoneId(zoneId);
        // 排号取库位码 '-' 前段（A1-01 → A1），与货架归属校验对齐
        l.setRackNo(code.contains("-") ? code.substring(0, code.indexOf('-')) : code);
        return l;
    }

    private WmsReturnQcItem item(String sku, int received) {
        WmsReturnQcItem it = new WmsReturnQcItem();
        it.setReturnOrderId(1L);
        it.setSkuCode(sku);
        it.setElectronic(0);
        it.setReceivedQty(received);
        return it;
    }

    @Test
    void receive_rejects_when_not_pending() {
        when(returnInboundMapper.selectById(1L)).thenReturn(order(ReturnQcStatus.QC_PENDING.name()));
        ReturnReceiveDTO dto = new ReturnReceiveDTO();
        dto.setReturnOrderId(1L);
        ReturnReceiveDTO.ReceiveLine l = new ReturnReceiveDTO.ReceiveLine();
        l.setSkuCode("SKU1");
        l.setReceivedQty(5);
        dto.setItems(Collections.singletonList(l));
        assertThatThrownBy(() -> service.receive(dto)).isInstanceOf(BusinessException.class);
    }

    @Test
    void receive_creates_items_and_sets_qc_pending() {
        when(returnInboundMapper.selectById(1L)).thenReturn(order(ReturnQcStatus.RETURN_PENDING.name()));
        when(returnInboundMapper.casReturnStatus(1L, ReturnQcStatus.RETURN_PENDING.name(),
                ReturnQcStatus.QC_PENDING.name())).thenReturn(1);
        when(returnInboundMapper.markReceivedAudit(1L, 88L)).thenReturn(1);
        when(itemMapper.selectByReturnOrderId(1L)).thenReturn(new ArrayList<>());
        ReturnReceiveDTO dto = new ReturnReceiveDTO();
        dto.setReturnOrderId(1L);
        ReturnReceiveDTO.ReceiveLine l = new ReturnReceiveDTO.ReceiveLine();
        l.setSkuCode("SKU1");
        l.setReceivedQty(5);
        dto.setItems(Collections.singletonList(l));

        service.receive(dto);

        verify(itemMapper).insert(any(WmsReturnQcItem.class));
        verify(returnInboundMapper).casReturnStatus(1L, ReturnQcStatus.RETURN_PENDING.name(),
                ReturnQcStatus.QC_PENDING.name());
        verify(returnInboundMapper).markReceivedAudit(1L, 88L);
    }

    @Test
    void qc_good_quantity_with_standard_zone_is_rejected() {
        when(returnInboundMapper.selectById(1L)).thenReturn(order(ReturnQcStatus.QC_PENDING.name()));
        when(returnInboundMapper.casReturnStatus(1L, ReturnQcStatus.QC_PENDING.name(),
                ReturnQcStatus.COMPLETED.name())).thenReturn(1);
        when(itemMapper.selectByReturnOrderId(1L)).thenReturn(Collections.singletonList(item("SKU1", 5)));
        when(zoneService.listByWarehouse(1L)).thenReturn(Arrays.asList(zone(8, "STANDARD"), zone(9, "DEFECTIVE")));

        ReturnQcDTO.ReturnQcLineDTO line = new ReturnQcDTO.ReturnQcLineDTO();
        line.setSkuCode("SKU1");
        line.setQualifiedQty(5);
        line.setDamagedQty(0);
        line.setQualifiedZone("STANDARD");
        line.setQualifiedLocationCode("A1-01");
        ReturnQcDTO dto = new ReturnQcDTO();
        dto.setReturnOrderId(1L);
        dto.setLines(Collections.singletonList(line));

        assertThatThrownBy(() -> service.qc(dto)).isInstanceOf(Exception.class);
    }

    @Test
    void qc_fail_generates_damaged_batch_and_completes() {
        when(returnInboundMapper.selectById(1L)).thenReturn(order(ReturnQcStatus.QC_PENDING.name()));
        when(returnInboundMapper.casReturnStatus(1L, ReturnQcStatus.QC_PENDING.name(),
                ReturnQcStatus.COMPLETED.name())).thenReturn(1);
        when(itemMapper.selectByReturnOrderId(1L)).thenReturn(Collections.singletonList(item("SKU1", 5)));
        when(itemMapper.updateById(any(WmsReturnQcItem.class))).thenReturn(1);
        when(zoneService.listByWarehouse(1L)).thenReturn(Arrays.asList(zone(8, "STANDARD"), zone(9, "DEFECTIVE")));
        // 回库库位存在于本仓、未占用、实际分区==DEFECTIVE(9)：满足 BUG#2 新增的三道库位守卫
        when(locationService.listByWarehouse(1L)).thenReturn(Collections.singletonList(location("D1-01", 9L)));

        ReturnQcDTO.ReturnQcLineDTO line = new ReturnQcDTO.ReturnQcLineDTO();
        line.setSkuCode("SKU1");
        line.setQualifiedQty(0);
        line.setDamagedQty(5);
        line.setDamagedLocationCode("D1-01");
        ReturnQcDTO dto = new ReturnQcDTO();
        dto.setReturnOrderId(1L);
        dto.setLines(Collections.singletonList(line));

        service.qc(dto);

        ArgumentCaptor<LocationInventoryKey> inventoryKey = ArgumentCaptor.forClass(LocationInventoryKey.class);
        verify(locationInventoryService).increase(inventoryKey.capture(), org.mockito.ArgumentMatchers.eq(5),
                org.mockito.ArgumentMatchers.any());
        assertThat(inventoryKey.getValue().getQuality()).isEqualTo("DEFECTIVE");
        assertThat(inventoryKey.getValue().getLocationId()).isEqualTo(2L);
        assertThat(inventoryKey.getValue().getErpTenantId()).isEqualTo(6L);

        ArgumentCaptor<ReturnInboundOrder> ocap = ArgumentCaptor.forClass(ReturnInboundOrder.class);
        verify(returnInboundMapper).updateById(ocap.capture());
        assertThat(ocap.getValue().getReturnStatus()).isEqualTo(ReturnQcStatus.COMPLETED.name());
        assertThat(ocap.getValue().getUnqualifiedQuantity()).isEqualTo(5);
        assertThat(ocap.getValue().getQcBy()).isEqualTo(88L);
        assertThat(ocap.getValue().getQcTime()).isNotNull();
    }

    @Test
    void qc_splits_good_and_damaged_into_two_batches() {
        when(returnInboundMapper.selectById(1L)).thenReturn(order(ReturnQcStatus.QC_PENDING.name()));
        when(returnInboundMapper.casReturnStatus(1L, ReturnQcStatus.QC_PENDING.name(),
                ReturnQcStatus.COMPLETED.name())).thenReturn(1);
        when(itemMapper.selectByReturnOrderId(1L)).thenReturn(Collections.singletonList(item("SKU1", 5)));
        when(itemMapper.updateById(any(WmsReturnQcItem.class))).thenReturn(1);
        when(zoneService.listByWarehouse(1L)).thenReturn(Arrays.asList(zone(7, "RETURN"), zone(9, "DEFECTIVE")));
        when(locationService.listByWarehouse(1L)).thenReturn(Arrays.asList(
                location("A1-01", 7L), location("D1-01", 9L)));

        ReturnQcDTO.ReturnQcLineDTO line = new ReturnQcDTO.ReturnQcLineDTO();
        line.setSkuCode("SKU1");
        line.setQualifiedQty(3);
        line.setDamagedQty(2);
        line.setQualifiedZone("RETURN");
        line.setQualifiedLocationCode("A1-01");
        line.setDamagedLocationCode("D1-01");
        ReturnQcDTO dto = new ReturnQcDTO();
        dto.setReturnOrderId(1L);
        dto.setLines(Collections.singletonList(line));

        service.qc(dto);

        ArgumentCaptor<LocationInventoryKey> keys = ArgumentCaptor.forClass(LocationInventoryKey.class);
        verify(locationInventoryService, times(2)).increase(keys.capture(), any(Integer.class),
                org.mockito.ArgumentMatchers.any());
        assertThat(keys.getAllValues()).extracting(LocationInventoryKey::getQuality)
                .containsExactly("GOOD", "DEFECTIVE");
        assertThat(keys.getAllValues()).extracting(LocationInventoryKey::getLocationId)
                .containsExactly(1L, 2L);
    }

    @Test
    void qc_rejects_electronic_damaged_goods_without_photo() {
        when(returnInboundMapper.selectById(1L)).thenReturn(order(ReturnQcStatus.QC_PENDING.name()));
        when(returnInboundMapper.casReturnStatus(1L, ReturnQcStatus.QC_PENDING.name(),
                ReturnQcStatus.COMPLETED.name())).thenReturn(1);
        WmsReturnQcItem electronicItem = item("SKU1", 5);
        electronicItem.setElectronic(1);
        when(itemMapper.selectByReturnOrderId(1L)).thenReturn(Collections.singletonList(electronicItem));
        when(zoneService.listByWarehouse(1L)).thenReturn(Collections.singletonList(zone(9, "DEFECTIVE")));

        ReturnQcDTO.ReturnQcLineDTO line = new ReturnQcDTO.ReturnQcLineDTO();
        line.setSkuCode("SKU1");
        line.setQualifiedQty(0);
        line.setDamagedQty(5);
        line.setDamagedLocationCode("D1-01");
        line.setPhotoFileIds(Collections.emptyList());
        ReturnQcDTO dto = new ReturnQcDTO();
        dto.setReturnOrderId(1L);
        dto.setLines(Collections.singletonList(line));

        assertThatThrownBy(() -> service.qc(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void close_only_advances_pending_return() {
        when(returnInboundMapper.selectById(1L)).thenReturn(order(ReturnQcStatus.RETURN_PENDING.name()));
        when(returnInboundMapper.casReturnStatus(1L, ReturnQcStatus.RETURN_PENDING.name(),
                ReturnQcStatus.CLOSED.name())).thenReturn(1);
        when(returnInboundMapper.markClosedAudit(1L, 88L)).thenReturn(1);

        service.close(1L);

        verify(returnInboundMapper).casReturnStatus(1L, ReturnQcStatus.RETURN_PENDING.name(),
                ReturnQcStatus.CLOSED.name());
        verify(returnInboundMapper).markClosedAudit(1L, 88L);
    }

    @Test
    void completed_qc_has_no_pallet_labels_in_logical_location_mode() {
        when(returnInboundMapper.selectById(1L)).thenReturn(order(ReturnQcStatus.COMPLETED.name()));
        WmsReturnQcItem item = item("SKU1", 5);
        item.setQualifiedPalletId(21L);
        item.setDamagedPalletId(22L);
        when(itemMapper.selectByReturnOrderId(1L)).thenReturn(Collections.singletonList(item));
        PalletSummaryVO good = new PalletSummaryVO();
        good.setId(21L);
        good.setPalletNo("PLT-B");
        PalletSummaryVO damaged = new PalletSummaryVO();
        damaged.setId(22L);
        damaged.setPalletNo("PLT-A");
        when(palletService.summaries(any())).thenReturn(Arrays.asList(good, damaged));

        List<PalletSummaryVO> pallets = service.listQcPallets(1L);

        assertThat(pallets).isEmpty();
    }

}
