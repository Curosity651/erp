package com.erp.admin.platform.dashboard.mapper;

import com.erp.admin.platform.dashboard.model.qo.PlatformDashboardQO;
import com.erp.admin.platform.dashboard.model.vo.DashboardRowVO;
import com.erp.admin.platform.dashboard.model.vo.PlatformDashboardDataVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 海外仓平台数据分析聚合 Mapper。全部为只读聚合，跨全部货主；
 * 仅当 qo.warehouseIds / qo.wmsTenantIds 非空时才收窄（服务商过滤经 erp_tenant_id→sys_tenant.parent_wms_tenant_id 映射）。
 *
 * @author erp
 */
public interface PlatformDashboardMapper {

    /** A 在库总量：件数 / SKU 数 / 货主数（wms_inventory 现存桶合计） */
    DashboardRowVO.StockRow selectOnHand(@Param("qo") PlatformDashboardQO qo);

    /** A 今日入库：件数(实收) + 单数（采购入库单，按 inbound_date=今日 且已收货/完成） */
    DashboardRowVO.MetricRow selectTodayInbound(@Param("qo") PlatformDashboardQO qo);

    /** A 今日出库：件数 + 单数（销售出库单，按 outbound_date=今日） */
    DashboardRowVO.MetricRow selectTodayOutbound(@Param("qo") PlatformDashboardQO qo);

    /** A 待收货单数（采购入库 SUBMITTED） */
    long countPendingReceiving(@Param("qo") PlatformDashboardQO qo);

    /** A 待上架单数（采购入库 RECEIVED） */
    long countPendingPutaway(@Param("qo") PlatformDashboardQO qo);

    /** A 待拣货打包单数（销售出库 CONFIRMED/PICKING） */
    long countPendingPickPack(@Param("qo") PlatformDashboardQO qo);

    /** A 待出库单数（销售出库 PICKED/PACKED） */
    long countPendingOutbound(@Param("qo") PlatformDashboardQO qo);

    /** B 各仓库容占用：total=库位数, used=已落位库位数 */
    List<PlatformDashboardDataVO.WarehouseCapacity> selectWarehouseCapacity(@Param("qo") PlatformDashboardQO qo);

    /** B 分区占用：locationCount=该分区库位数, invQty=该分区库存件数 */
    List<PlatformDashboardDataVO.ZoneOccupancy> selectZoneOccupancy(@Param("qo") PlatformDashboardQO qo);

    /** C 每日入库单数（区间内，按 inbound_date） */
    List<DashboardRowVO.DailyCountRow> selectInboundDaily(@Param("qo") PlatformDashboardQO qo);

    /** C 每日出库单数（区间内，按 outbound_date） */
    List<DashboardRowVO.DailyCountRow> selectOutboundDaily(@Param("qo") PlatformDashboardQO qo);

    /** C 每日退货单数（区间内，按 return_date） */
    List<DashboardRowVO.DailyCountRow> selectReturnDaily(@Param("qo") PlatformDashboardQO qo);

    /** D 服务商在库占用 TOP10（按服务商聚合，件数降序） */
    List<PlatformDashboardDataVO.OperatorRankingItem> selectOperatorByStock(@Param("qo") PlatformDashboardQO qo);

    /** D 服务商吞吐 TOP10（按服务商聚合，区间内入+出单数降序） */
    List<PlatformDashboardDataVO.OperatorRankingItem> selectOperatorByThroughput(@Param("qo") PlatformDashboardQO qo);

}
