package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.vo.OperatorDashboardVO;
import org.apache.ibatis.annotations.Param;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * WMS 服务商运营看板聚合 Mapper。收入按 wms_tenant_id 收窄；货主域数据一律
 * JOIN sys_tenant (parent_wms_tenant_id=自己) 限定名下，不依赖调用方传作用域。
 *
 * @author erp
 */
public interface OperatorDashboardMapper {

    /** 收入按月（金额+次数）；rows: billMonth/amount/cnt */
    List<Map<String, Object>> sumIncomeByMonth(@Param("wmsTenantId") Long wmsTenantId,
            @Param("monthStart") String monthStart, @Param("monthEnd") String monthEnd,
            @Param("erpTenantIds") List<Long> erpTenantIds);

    /** 支出按月（月度账单合计）；rows: billMonth/amount */
    List<Map<String, Object>> sumExpenseByMonth(@Param("wmsTenantId") Long wmsTenantId,
            @Param("monthStart") String monthStart, @Param("monthEnd") String monthEnd);

    /** 按产品聚合（次数+金额，产品名由 service 补） */
    List<OperatorDashboardVO.ProductStat> sumByProduct(@Param("wmsTenantId") Long wmsTenantId,
            @Param("monthStart") String monthStart, @Param("monthEnd") String monthEnd,
            @Param("erpTenantIds") List<Long> erpTenantIds);

    /** 货主收入贡献 TOP10 */
    List<OperatorDashboardVO.OwnerStat> sumIncomeByOwner(@Param("wmsTenantId") Long wmsTenantId,
            @Param("monthStart") String monthStart, @Param("monthEnd") String monthEnd,
            @Param("erpTenantIds") List<Long> erpTenantIds);

    /** 货主出库吞吐 TOP10（名下货主，区间内非取消出库单数） */
    List<OperatorDashboardVO.OwnerOrderStat> countOutboundByOwner(@Param("wmsTenantId") Long wmsTenantId,
            @Param("dateStart") LocalDate dateStart, @Param("dateEnd") LocalDate dateEnd,
            @Param("erpTenantIds") List<Long> erpTenantIds);

    /** 名下货主数；row: total/enabled */
    Map<String, Object> countOwners(@Param("wmsTenantId") Long wmsTenantId);

    /** 物流产品数；row: total/enabled */
    Map<String, Object> countProducts(@Param("wmsTenantId") Long wmsTenantId);

    /** 名下货主在库总件数 */
    Long sumOwnerOnHand(@Param("wmsTenantId") Long wmsTenantId, @Param("erpTenantIds") List<Long> erpTenantIds);

    /** 当前有效货架数 + 月租合计；row: cnt/fee */
    Map<String, Object> sumRacks(@Param("wmsTenantId") Long wmsTenantId, @Param("today") LocalDate today);

}
