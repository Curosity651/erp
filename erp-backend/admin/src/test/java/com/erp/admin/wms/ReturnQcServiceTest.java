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
import com.erp.admin.wms.model.dto.ReturnDispositionDTO;
import com.erp.admin.wms.model.dto.ReturnProcessDTO;
import com.erp.admin.wms.model.dto.ReturnReceiptDTO;
import com.erp.admin.wms.model.entity.ReturnInboundOrder;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsReturnQcItem;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.model.enums.ReturnQcStatus;
import com.erp.admin.wms.model.vo.PalletSummaryVO;
import com.erp.admin.wms.model.vo.SkuLookupVO;
import com.erp.admin.wms.model.vo.WarehouseSkuResolveVO;
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
import static org.mockito.Mockito.never;

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
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;
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
        stringRedisTemplate = mock(org.springframework.data.redis.core.StringRedisTemplate.class);
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
                skuLookupMapper, sysFileService, warehouseSkuCodeService, principalAttributeAccessor,
                stringRedisTemplate);
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
    void close_only_advances_pending_owner_disposition() {
        when(returnInboundMapper.selectById(1L)).thenReturn(order(ReturnQcStatus.PENDING_OWNER.name()));
        when(returnInboundMapper.casReturnStatus(1L, ReturnQcStatus.PENDING_OWNER.name(),
                ReturnQcStatus.CLOSED.name())).thenReturn(1);
        when(returnInboundMapper.markClosedAudit(1L, 88L)).thenReturn(1);

        service.close(1L);

        verify(returnInboundMapper).casReturnStatus(1L, ReturnQcStatus.PENDING_OWNER.name(),
                ReturnQcStatus.CLOSED.name());
        verify(returnInboundMapper).markClosedAudit(1L, 88L);
    }

    @Test
    void owner_disposition_rejects_quantity_mismatch() {
        TenantIdentityVO ownerIdentity = new TenantIdentityVO();
        ownerIdentity.setIdentityType(TenantIdentityService.IDENTITY_ERP_USER);
        ownerIdentity.setTenantId(6L);
        when(tis.currentIdentity(TenantIdentityService.IDENTITY_ERP_USER)).thenReturn(ownerIdentity);
        ReturnInboundOrder order = order(ReturnQcStatus.PENDING_OWNER.name());
        order.setErpTenantId(6L);
        when(returnInboundMapper.selectById(1L)).thenReturn(order);
        WmsReturnQcItem item = item("SKU1", 5);
        item.setId(10L);
        when(itemMapper.selectByReturnOrderId(1L)).thenReturn(Collections.singletonList(item));
        ReturnDispositionDTO.Line line = new ReturnDispositionDTO.Line();
        line.setItemId(10L);
        line.setRestockQty(2);
        line.setReworkQty(0);
        line.setScrapQty(0);
        ReturnDispositionDTO dto = new ReturnDispositionDTO();
        dto.setReturnOrderId(1L);
        dto.setItems(Collections.singletonList(line));

        assertThatThrownBy(() -> service.submitDisposition(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("处置数量之和");
    }

    @Test
    void warehouse_process_records_destroyed_return_without_putaway() {
        ReturnInboundOrder order = order(ReturnQcStatus.PENDING_OPERATION.name());
        order.setErpTenantId(6L);
        when(returnInboundMapper.selectById(1L)).thenReturn(order);
        when(returnInboundMapper.casReturnStatus(1L, ReturnQcStatus.PENDING_OPERATION.name(),
                ReturnQcStatus.COMPLETED.name())).thenReturn(1);
        when(returnInboundMapper.updateById(any())).thenReturn(1);
        WmsReturnQcItem item = item("SKU1", 2);
        item.setId(10L);
        item.setRestockQty(0);
        item.setReworkQty(0);
        item.setScrapQty(2);
        when(itemMapper.selectByReturnOrderId(1L)).thenReturn(Collections.singletonList(item));
        when(itemMapper.updateById(any())).thenReturn(1);
        ReturnProcessDTO.Line line = new ReturnProcessDTO.Line();
        line.setItemId(10L);
        line.setReworkPassQty(0);
        line.setReworkScrapQty(0);
        ReturnProcessDTO dto = new ReturnProcessDTO();
        dto.setReturnOrderId(1L);
        dto.setItems(Collections.singletonList(line));

        service.processDisposition(dto);

        assertThat(order.getScrapQuantity()).isEqualTo(2);
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

    @Test
    void exposes_global_warehouse_sku_resolver_for_return_receipts() {
        assertThat(Arrays.stream(ReturnQcService.class.getMethods())
                .map(java.lang.reflect.Method::getName))
                .contains("resolveWarehouseSkus");
    }

    @Test
    void resolves_global_warehouse_sku_to_canonical_product_and_owner() {
        SkuLookupVO sku = warehouseSku("JHIN-SKU-1", 6L, "SKU-1", "测试商品");
        sku.setOwnerName("JHIN货主");
        when(skuLookupMapper.findByWarehouseSkuCodes(any())).thenReturn(Collections.singletonList(sku));

        List<WarehouseSkuResolveVO> results = service.resolveWarehouseSkus(
                Collections.singletonList(" jhin-sku-1 "));

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.isMatched()).isTrue();
            assertThat(result.getRequestedCode()).isEqualTo("jhin-sku-1");
            assertThat(result.getWarehouseSkuCode()).isEqualTo("JHIN-SKU-1");
            assertThat(result.getErpTenantId()).isEqualTo(6L);
            assertThat(result.getOwnerName()).isEqualTo("JHIN货主");
            assertThat(result.getSkuCode()).isEqualTo("SKU-1");
            assertThat(result.getSkuName()).isEqualTo("测试商品");
        });
    }

    @Test
    void resolver_marks_unknown_global_sku_without_inventing_identity() {
        when(skuLookupMapper.findByWarehouseSkuCodes(any())).thenReturn(Collections.emptyList());

        assertThat(service.resolveWarehouseSkus(Collections.singletonList("UNKNOWN-SKU")))
                .singleElement().satisfies(result -> {
                    assertThat(result.isMatched()).isFalse();
                    assertThat(result.getError()).contains("不存在");
                    assertThat(result.getErpTenantId()).isNull();
                    assertThat(result.getSkuCode()).isNull();
                });
    }

    @Test
    void resolver_rejects_duplicate_normalized_global_skus() {
        assertThatThrownBy(() -> service.resolveWarehouseSkus(
                Arrays.asList("JHIN-SKU-1", " jhin-sku-1 ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("重复");
    }

    @Test
    void resolver_rejects_more_than_500_global_skus() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            codes.add("JHIN-SKU-" + i);
        }
        assertThatThrownBy(() -> service.resolveWarehouseSkus(codes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");
    }

    @Test
    void receipt_derives_owner_and_original_sku_from_global_sku() {
        prepareReceiptPersistence();
        when(skuLookupMapper.findByWarehouseSkuCodes(any())).thenReturn(Collections.singletonList(
                warehouseSku("JHIN-SKU-1", 6L, "SKU-1", "测试商品")));
        ReturnReceiptDTO dto = receiptDto(line("JHIN-SKU-1", 3));

        service.registerReceipt(dto);

        ArgumentCaptor<ReturnInboundOrder> orderCaptor = ArgumentCaptor.forClass(ReturnInboundOrder.class);
        ArgumentCaptor<WmsReturnQcItem> itemCaptor = ArgumentCaptor.forClass(WmsReturnQcItem.class);
        verify(returnInboundMapper).insert(orderCaptor.capture());
        verify(itemMapper).insert(itemCaptor.capture());
        assertThat(orderCaptor.getValue().getErpTenantId()).isEqualTo(6L);
        assertThat(orderCaptor.getValue().getSkuCode()).isEqualTo("SKU-1");
        assertThat(itemCaptor.getValue().getSkuCode()).isEqualTo("SKU-1");
    }

    @Test
    void receipt_splits_resolved_global_skus_into_one_order_per_owner() {
        prepareReceiptPersistence();
        when(skuLookupMapper.findByWarehouseSkuCodes(any())).thenReturn(Arrays.asList(
                warehouseSku("JHIN-SKU-1", 6L, "SKU-1", "商品1"),
                warehouseSku("ACME-SKU-2", 7L, "SKU-2", "商品2")));
        SysTenant owner6 = new SysTenant();
        owner6.setParentWmsTenantId(5L);
        owner6.setTenantName("JHIN");
        SysTenant owner7 = new SysTenant();
        owner7.setParentWmsTenantId(5L);
        owner7.setTenantName("ACME");
        when(sysTenantMapper.selectById(6L)).thenReturn(owner6);
        when(sysTenantMapper.selectById(7L)).thenReturn(owner7);

        List<Long> ids = service.registerReceipt(receiptDto(
                line("JHIN-SKU-1", 2), line("ACME-SKU-2", 4)));

        assertThat(ids).hasSize(2);
        ArgumentCaptor<ReturnInboundOrder> orders = ArgumentCaptor.forClass(ReturnInboundOrder.class);
        verify(returnInboundMapper, times(2)).insert(orders.capture());
        assertThat(orders.getAllValues()).extracting(ReturnInboundOrder::getErpTenantId)
                .containsExactly(6L, 7L);
    }

    @Test
    void receipt_rejects_unknown_global_sku_before_any_insert() {
        when(skuLookupMapper.findByWarehouseSkuCodes(any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.registerReceipt(receiptDto(line("UNKNOWN-SKU", 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不存在");
        verify(returnInboundMapper, never()).insert(any());
        verify(itemMapper, never()).insert(any());
    }

    private SkuLookupVO warehouseSku(String warehouseSkuCode, Long ownerId, String skuCode, String name) {
        SkuLookupVO sku = new SkuLookupVO();
        sku.setWarehouseSkuCode(warehouseSkuCode);
        sku.setErpTenantId(ownerId);
        sku.setSkuCode(skuCode);
        sku.setChineseName(name);
        return sku;
    }

    private ReturnReceiptDTO.Line line(String warehouseSkuCode, int quantity) {
        ReturnReceiptDTO.Line line = new ReturnReceiptDTO.Line();
        line.setWarehouseSkuCode(warehouseSkuCode);
        line.setReceivedQty(quantity);
        return line;
    }

    private ReturnReceiptDTO receiptDto(ReturnReceiptDTO.Line... lines) {
        ReturnReceiptDTO dto = new ReturnReceiptDTO();
        dto.setWarehouseId(1L);
        dto.setItems(Arrays.asList(lines));
        return dto;
    }

    @SuppressWarnings("unchecked")
    private void prepareReceiptPersistence() {
        org.springframework.data.redis.core.ValueOperations<String, String> values =
                mock(org.springframework.data.redis.core.ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(values);
        when(values.increment(any())).thenReturn(1L, 1L, 2L, 2L, 3L, 3L);
        final long[] nextId = {100L};
        when(returnInboundMapper.insert(any(ReturnInboundOrder.class))).thenAnswer(invocation -> {
            ReturnInboundOrder order = invocation.getArgument(0);
            order.setId(nextId[0]++);
            return 1;
        });
        when(itemMapper.insert(any(WmsReturnQcItem.class))).thenReturn(1);
    }

}
