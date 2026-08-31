package com.erp.admin.wms;

import com.erp.admin.wms.mapper.RegionMapper;
import com.erp.admin.wms.model.dto.WarehouseAggregateDTO;
import com.erp.admin.wms.model.entity.Region;
import com.erp.admin.wms.model.vo.RegionSummaryVO;
import com.erp.admin.wms.service.OwnerInventoryQueryService;
import com.erp.admin.wms.service.RegionInventoryService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegionInventoryServiceTest {

    @Test
    void summary_uses_own_warehouse_snapshot_only() {
        RegionMapper regionMapper = mock(RegionMapper.class);
        OwnerInventoryQueryService inventoryQueryService = mock(OwnerInventoryQueryService.class);
        RegionInventoryService service = new RegionInventoryService(
                regionMapper, inventoryQueryService);

        Region region = new Region();
        region.setId(1L);
        region.setRegionCode("RU");
        region.setRegionName("Russia");

        WarehouseAggregateDTO aggregate = new WarehouseAggregateDTO();
        aggregate.setRegionId(1L);
        aggregate.setWarehouseCount(2);
        aggregate.setTotalAvailable(40);
        aggregate.setTotalReserved(6);
        aggregate.setTotalInTransit(3);
        aggregate.setTotalDamaged(2);

        when(regionMapper.selectEnabledList()).thenReturn(Collections.singletonList(region));
        when(inventoryQueryService.getRegionAggregates(anySet()))
                .thenReturn(Collections.singletonList(aggregate));

        RegionSummaryVO summary = service.getRegionSummaryList().get(0);

        assertThat(summary.getRegionAvailable()).isEqualTo(40);
        assertThat(summary.getRegionReserved()).isEqualTo(6);
        assertThat(summary.getRegionInTransit()).isEqualTo(3);
        assertThat(summary.getRegionDamaged()).isEqualTo(2);
    }

}
