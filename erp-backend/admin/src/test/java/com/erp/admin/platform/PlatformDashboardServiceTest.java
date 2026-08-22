package com.erp.admin.platform;

import com.erp.admin.platform.dashboard.mapper.PlatformDashboardMapper;
import com.erp.admin.platform.dashboard.model.qo.PlatformDashboardQO;
import com.erp.admin.platform.dashboard.model.vo.DashboardRowVO;
import com.erp.admin.platform.dashboard.model.vo.PlatformDashboardDataVO;
import com.erp.admin.platform.dashboard.service.PlatformDashboardService;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import org.ballcat.common.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 平台数据分析聚合服务测试：身份守卫 + 分区补齐(固定4类) + 吞吐日期轴填充。
 *
 * @author erp
 */
class PlatformDashboardServiceTest {

    private PlatformDashboardMapper mapper;

    private TenantIdentityService tenantIdentityService;

    private PlatformDashboardService service;

    @BeforeEach
    void setUp() {
        mapper = mock(PlatformDashboardMapper.class);
        tenantIdentityService = mock(TenantIdentityService.class);
        service = new PlatformDashboardService(mapper, tenantIdentityService);
    }

    private void asPlatform() {
        TenantIdentityVO id = mock(TenantIdentityVO.class);
        when(id.getIdentityType()).thenReturn(TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM);
        when(tenantIdentityService.currentIdentity(any())).thenReturn(id);
    }

    private DashboardRowVO.MetricRow metric(long qty, long orders) {
        DashboardRowVO.MetricRow m = new DashboardRowVO.MetricRow();
        m.setQty(qty);
        m.setOrders(orders);
        return m;
    }

    private DashboardRowVO.DailyCountRow daily(String d, long c) {
        DashboardRowVO.DailyCountRow r = new DashboardRowVO.DailyCountRow();
        r.setBizDate(d);
        r.setCnt(c);
        return r;
    }

    @Test
    void non_platform_identity_is_forbidden() {
        TenantIdentityVO id = mock(TenantIdentityVO.class);
        when(id.getIdentityType()).thenReturn("ERP_USER");
        when(tenantIdentityService.currentIdentity(any())).thenReturn(id);

        assertThatThrownBy(() -> service.getData(new PlatformDashboardQO()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void zones_filled_to_four_canonical_and_dates_axis_built() {
        asPlatform();
        DashboardRowVO.StockRow stock = new DashboardRowVO.StockRow();
        stock.setOnHandQty(100);
        stock.setSkuCount(5);
        stock.setOwnerCount(2);
        when(mapper.selectOnHand(any())).thenReturn(stock);
        when(mapper.selectTodayInbound(any())).thenReturn(metric(10, 1));
        when(mapper.selectTodayOutbound(any())).thenReturn(metric(20, 2));
        when(mapper.selectWarehouseCapacity(any())).thenReturn(Collections.emptyList());

        // 仅 STANDARD 有数据，其余三类应被补 0
        PlatformDashboardDataVO.ZoneOccupancy std = new PlatformDashboardDataVO.ZoneOccupancy();
        std.setZone("STANDARD");
        std.setLocationCount(8);
        std.setInvQty(80);
        when(mapper.selectZoneOccupancy(any())).thenReturn(Collections.singletonList(std));

        when(mapper.selectInboundDaily(any()))
                .thenReturn(Collections.singletonList(daily("2026-06-02", 3)));
        when(mapper.selectOutboundDaily(any())).thenReturn(Collections.emptyList());
        when(mapper.selectReturnDaily(any())).thenReturn(Collections.emptyList());
        when(mapper.selectOperatorByStock(any())).thenReturn(Collections.emptyList());
        when(mapper.selectOperatorByThroughput(any())).thenReturn(Collections.emptyList());

        PlatformDashboardQO qo = new PlatformDashboardQO();
        qo.setStartDate(LocalDate.of(2026, 6, 1));
        qo.setEndDate(LocalDate.of(2026, 6, 3));

        PlatformDashboardDataVO vo = service.getData(qo);

        // A
        assertThat(vo.getOpsOverview().getOnHandQty()).isEqualTo(100);
        assertThat(vo.getOpsOverview().getTodayOutboundOrders()).isEqualTo(2);

        // B 分区固定 4 类且顺序正确
        assertThat(vo.getCapacity().getByZone()).extracting(PlatformDashboardDataVO.ZoneOccupancy::getZone)
                .containsExactly("STANDARD", "DEFECTIVE", "RETURN", "TEMP");
        assertThat(vo.getCapacity().getByZone().get(0).getInvQty()).isEqualTo(80);
        assertThat(vo.getCapacity().getByZone().get(1).getInvQty()).isEqualTo(0);

        // C 日期轴 3 天，对齐填充
        assertThat(vo.getThroughput().getDates()).containsExactly("2026-06-01", "2026-06-02", "2026-06-03");
        assertThat(vo.getThroughput().getInbound()).containsExactly(0L, 3L, 0L);
        assertThat(vo.getThroughput().getOutbound()).containsExactly(0L, 0L, 0L);
    }

    @Test
    void default_date_range_is_last_seven_days() {
        asPlatform();
        when(mapper.selectOnHand(any())).thenReturn(new DashboardRowVO.StockRow());
        when(mapper.selectTodayInbound(any())).thenReturn(metric(0, 0));
        when(mapper.selectTodayOutbound(any())).thenReturn(metric(0, 0));
        when(mapper.selectWarehouseCapacity(any())).thenReturn(Collections.emptyList());
        when(mapper.selectZoneOccupancy(any())).thenReturn(Collections.emptyList());
        when(mapper.selectInboundDaily(any())).thenReturn(Collections.emptyList());
        when(mapper.selectOutboundDaily(any())).thenReturn(Collections.emptyList());
        when(mapper.selectReturnDaily(any())).thenReturn(Collections.emptyList());
        when(mapper.selectOperatorByStock(any())).thenReturn(Collections.emptyList());
        when(mapper.selectOperatorByThroughput(any())).thenReturn(Collections.emptyList());

        PlatformDashboardDataVO vo = service.getData(new PlatformDashboardQO());
        // 默认最近 7 天 → 日期轴 7 个点，全 4 类分区
        assertThat(vo.getThroughput().getDates()).hasSize(7);
        assertThat(vo.getCapacity().getByZone()).hasSize(4);
        assertThat(Arrays.asList("STANDARD", "DEFECTIVE", "RETURN", "TEMP"))
                .containsExactlyElementsOf(
                        vo.getCapacity().getByZone().stream()
                                .map(PlatformDashboardDataVO.ZoneOccupancy::getZone)
                                .collect(java.util.stream.Collectors.toList()));
    }

    @Test
    void dashboard_queries_use_moscow_business_date() {
        asPlatform();
        when(mapper.selectOnHand(any())).thenReturn(new DashboardRowVO.StockRow());
        when(mapper.selectTodayInbound(any())).thenReturn(metric(0, 0));
        when(mapper.selectTodayOutbound(any())).thenReturn(metric(0, 0));
        when(mapper.selectWarehouseCapacity(any())).thenReturn(Collections.emptyList());
        when(mapper.selectZoneOccupancy(any())).thenReturn(Collections.emptyList());
        when(mapper.selectInboundDaily(any())).thenReturn(Collections.emptyList());
        when(mapper.selectOutboundDaily(any())).thenReturn(Collections.emptyList());
        when(mapper.selectReturnDaily(any())).thenReturn(Collections.emptyList());
        when(mapper.selectOperatorByStock(any())).thenReturn(Collections.emptyList());
        when(mapper.selectOperatorByThroughput(any())).thenReturn(Collections.emptyList());

        PlatformDashboardQO qo = new PlatformDashboardQO();
        service.getData(qo);

        verify(mapper, atLeastOnce()).selectTodayOutbound(qo);
        assertThat(qo.getTodayDate()).isEqualTo(LocalDate.now(ZoneId.of("Europe/Moscow")));
    }

}
