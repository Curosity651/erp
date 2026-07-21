package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.vo.OperatorIncomeVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 服务商财务读模型 Mapper（收入=链路二 wms_client_billing_record，按当前服务商 wms_tenant_id 收窄）。
 *
 * @author erp
 */
public interface OperatorFinanceMapper {

    /** 收入按 产品×月 聚合（次数 + 金额小计）；产品名/词条由 service 补 */
    List<OperatorIncomeVO.SummaryRow> sumIncomeByProductMonth(@Param("wmsTenantId") Long wmsTenantId,
            @Param("monthStart") String monthStart, @Param("monthEnd") String monthEnd,
            @Param("erpTenantId") Long erpTenantId);

    /** 收入明细流水（含货主名/出库单号） */
    List<OperatorIncomeVO.Record> listIncomeRecords(@Param("wmsTenantId") Long wmsTenantId,
            @Param("monthStart") String monthStart, @Param("monthEnd") String monthEnd,
            @Param("erpTenantId") Long erpTenantId, @Param("productId") Long productId);

}
