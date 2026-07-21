package com.erp.admin.wms;

import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.ReturnInboundMapper;
import com.erp.admin.wms.model.dto.ReturnInboundDTO;
import com.erp.admin.wms.model.entity.ReturnInboundOrder;
import com.erp.admin.wms.model.vo.ReturnOrderSourceVO;
import com.erp.admin.wms.service.ReturnInboundService;
import com.erp.admin.wms.service.WarehouseService;
import com.erp.admin.wms.service.WmsRackAssignmentService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReturnInboundServiceTest {

    @SuppressWarnings("unchecked")
    @Test
    void create_uses_locked_server_side_order_relationship() {
        ReturnInboundMapper mapper = mock(ReturnInboundMapper.class);
        SkuBriefService skuBriefService = mock(SkuBriefService.class);
        TenantIdentityService identityService = mock(TenantIdentityService.class);
        SysTenantMapper tenantMapper = mock(SysTenantMapper.class);
        WarehouseService warehouseService = mock(WarehouseService.class);
        WmsRackAssignmentService rackService = mock(WmsRackAssignmentService.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);

        TenantIdentityVO identity = new TenantIdentityVO();
        identity.setTenantId(6L);
        when(identityService.currentIdentity(TenantIdentityService.IDENTITY_ERP_USER)).thenReturn(identity);
        SysTenant owner = new SysTenant();
        owner.setParentWmsTenantId(5L);
        when(tenantMapper.selectById(6L)).thenReturn(owner);
        when(warehouseService.getById(2L)).thenReturn(new com.erp.admin.wms.model.entity.Warehouse());
        when(rackService.activeRackNos(2L, 5L)).thenReturn(Collections.singleton("A1"));

        ReturnOrderSourceVO source = new ReturnOrderSourceVO();
        source.setOrderId(100L);
        source.setOrderItemId(200L);
        source.setErpTenantId(6L);
        source.setPlatform("ozon");
        source.setPlatformOrderId("OZ-100");
        source.setOutboundStatus("COMPLETED");
        source.setSkuCode("SKU-REAL");
        source.setShippedQuantity(10);
        source.setReturnedQuantity(2);
        when(mapper.selectSourceForUpdate(200L, 6L)).thenReturn(source);
        when(mapper.selectList(any())).thenReturn(Collections.emptyList());
        when(mapper.insert(any(ReturnInboundOrder.class))).thenReturn(1);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(any(String.class))).thenReturn(1L);

        ReturnInboundService service = new ReturnInboundService(skuBriefService, identityService,
                tenantMapper, warehouseService, rackService, redis);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        ReturnInboundDTO dto = new ReturnInboundDTO();
        dto.setOrderItemId(200L);
        dto.setWarehouseId(2L);
        dto.setReturnDate(LocalDate.now());
        dto.setReturnReason("NOT_WANTED");
        dto.setQuantity(3);
        service.create(dto);

        ArgumentCaptor<ReturnInboundOrder> saved = ArgumentCaptor.forClass(ReturnInboundOrder.class);
        verify(mapper).insert(saved.capture());
        assertThat(saved.getValue().getErpTenantId()).isEqualTo(6L);
        assertThat(saved.getValue().getErpOrderId()).isEqualTo(100L);
        assertThat(saved.getValue().getPlatformOrderId()).isEqualTo("OZ-100");
        assertThat(saved.getValue().getSkuCode()).isEqualTo("SKU-REAL");
        assertThat(saved.getValue().getReturnableQuantity()).isEqualTo(8);
    }

}
