package com.erp.admin.platform.dashboard.service;

import com.erp.admin.platform.dashboard.mapper.PlatformDashboardMapper;
import com.erp.admin.platform.dashboard.model.qo.PlatformDashboardQO;
import com.erp.admin.platform.dashboard.model.vo.DashboardRowVO;
import com.erp.admin.platform.dashboard.model.vo.PlatformDashboardDataVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 海外仓平台数据分析聚合服务（只读，跨全部货主；仅当过滤集合非空时收窄）。
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class PlatformDashboardService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");

    /** 分区固定顺序（与前端图例一致），无数据补 0 */
    private static final List<String> ZONE_ORDER = Arrays.asList("STANDARD", "DEFECTIVE", "RETURN", "TEMP");

    private final PlatformDashboardMapper mapper;

    private final TenantIdentityService tenantIdentityService;

    public PlatformDashboardDataVO getData(PlatformDashboardQO qo) {
        // 平台看板跨全部货主聚合，仅海外仓平台身份可访问，防止服务商/货主越权读取全量
        String identityType = tenantIdentityService.currentIdentity(null).getIdentityType();
        if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(identityType)) {
            throw new BusinessException(403, "仅海外仓平台可访问平台数据分析");
        }
        // 看板业务日统一使用莫斯科日期，避免北京时间凌晨产生 5 小时错位。
        LocalDate today = LocalDate.now(MOSCOW_ZONE);
        qo.setTodayDate(today);
        // 日期兜底：默认最近 7 天
        if (qo.getEndDate() == null) {
            qo.setEndDate(today);
        }
        if (qo.getStartDate() == null) {
            qo.setStartDate(qo.getEndDate().minusDays(6));
        }
        if (qo.getStartDate().isAfter(qo.getEndDate())) {
            LocalDate t = qo.getStartDate();
            qo.setStartDate(qo.getEndDate());
            qo.setEndDate(t);
        }

        PlatformDashboardDataVO vo = new PlatformDashboardDataVO();
        vo.setOpsOverview(buildOpsOverview(qo));
        vo.setCapacity(buildCapacity(qo));
        vo.setThroughput(buildThroughput(qo));
        vo.setOperatorRanking(buildOperatorRanking(qo));
        return vo;
    }

    private PlatformDashboardDataVO.OpsOverview buildOpsOverview(PlatformDashboardQO qo) {
        PlatformDashboardDataVO.OpsOverview o = new PlatformDashboardDataVO.OpsOverview();
        DashboardRowVO.StockRow stock = mapper.selectOnHand(qo);
        if (stock != null) {
            o.setOnHandQty(stock.getOnHandQty());
            o.setSkuCount(stock.getSkuCount());
            o.setOwnerCount(stock.getOwnerCount());
        }
        DashboardRowVO.MetricRow in = mapper.selectTodayInbound(qo);
        if (in != null) {
            o.setTodayInboundQty(in.getQty());
            o.setTodayInboundOrders(in.getOrders());
        }
        DashboardRowVO.MetricRow out = mapper.selectTodayOutbound(qo);
        if (out != null) {
            o.setTodayOutboundQty(out.getQty());
            o.setTodayOutboundOrders(out.getOrders());
        }
        PlatformDashboardDataVO.Pending p = new PlatformDashboardDataVO.Pending();
        p.setReceiving(mapper.countPendingReceiving(qo));
        p.setPutaway(mapper.countPendingPutaway(qo));
        p.setPickPack(mapper.countPendingPickPack(qo));
        p.setOutbound(mapper.countPendingOutbound(qo));
        o.setPending(p);
        return o;
    }

    private PlatformDashboardDataVO.Capacity buildCapacity(PlatformDashboardQO qo) {
        PlatformDashboardDataVO.Capacity c = new PlatformDashboardDataVO.Capacity();
        c.setByWarehouse(mapper.selectWarehouseCapacity(qo));

        // 分区补齐固定 4 类并按顺序输出
        Map<String, PlatformDashboardDataVO.ZoneOccupancy> byType = mapper.selectZoneOccupancy(qo).stream()
                .filter(z -> z.getZone() != null)
                .collect(Collectors.toMap(PlatformDashboardDataVO.ZoneOccupancy::getZone, z -> z, (a, b) -> a));
        List<PlatformDashboardDataVO.ZoneOccupancy> zones = new ArrayList<>();
        for (String type : ZONE_ORDER) {
            PlatformDashboardDataVO.ZoneOccupancy z = byType.get(type);
            if (z == null) {
                z = new PlatformDashboardDataVO.ZoneOccupancy();
                z.setZone(type);
                z.setLocationCount(0);
                z.setInvQty(0);
            }
            zones.add(z);
        }
        c.setByZone(zones);
        return c;
    }

    private PlatformDashboardDataVO.ThroughputTrend buildThroughput(PlatformDashboardQO qo) {
        Map<String, Long> in = toDayMap(mapper.selectInboundDaily(qo));
        Map<String, Long> out = toDayMap(mapper.selectOutboundDaily(qo));
        Map<String, Long> ret = toDayMap(mapper.selectReturnDaily(qo));

        List<String> dates = new ArrayList<>();
        List<Long> inboundArr = new ArrayList<>();
        List<Long> outboundArr = new ArrayList<>();
        List<Long> returnsArr = new ArrayList<>();
        for (LocalDate d = qo.getStartDate(); !d.isAfter(qo.getEndDate()); d = d.plusDays(1)) {
            String key = d.format(DAY);
            dates.add(key);
            inboundArr.add(in.getOrDefault(key, 0L));
            outboundArr.add(out.getOrDefault(key, 0L));
            returnsArr.add(ret.getOrDefault(key, 0L));
        }
        PlatformDashboardDataVO.ThroughputTrend t = new PlatformDashboardDataVO.ThroughputTrend();
        t.setDates(dates);
        t.setInbound(inboundArr);
        t.setOutbound(outboundArr);
        t.setReturns(returnsArr);
        return t;
    }

    private PlatformDashboardDataVO.OperatorRanking buildOperatorRanking(PlatformDashboardQO qo) {
        PlatformDashboardDataVO.OperatorRanking r = new PlatformDashboardDataVO.OperatorRanking();
        r.setByStock(mapper.selectOperatorByStock(qo));
        r.setByThroughput(mapper.selectOperatorByThroughput(qo));
        return r;
    }

    private Map<String, Long> toDayMap(List<DashboardRowVO.DailyCountRow> rows) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (DashboardRowVO.DailyCountRow row : rows) {
            if (row.getBizDate() != null) {
                m.put(row.getBizDate(), row.getCnt());
            }
        }
        return m;
    }

}
