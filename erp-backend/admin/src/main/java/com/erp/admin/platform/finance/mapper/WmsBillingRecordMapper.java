package com.erp.admin.platform.finance.mapper;

import com.erp.admin.platform.finance.model.entity.WmsBillingRecord;
import com.erp.admin.platform.finance.model.vo.BillingRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

@Mapper
public interface WmsBillingRecordMapper extends ExtendMapper<WmsBillingRecord> {

    default WmsBillingRecord selectByBizId(String bizId) {
        return selectOne(WrappersX.lambdaQueryX(WmsBillingRecord.class)
                .eq(WmsBillingRecord::getBizId, bizId)
                .last("LIMIT 1"));
    }

    @Select("SELECT r.id, r.erp_tenant_id AS erpTenantId, t.tenant_name AS ownerName, "
            + "r.warehouse_id AS warehouseId, r.fee_type AS feeType, r.fee_code AS feeCode, "
            + "COALESCE(NULLIF(SUBSTRING_INDEX(r.rate_snapshot, '|', 1), ''), r.fee_code) AS feeName, "
            + "r.billing_unit AS billingUnit, r.billing_quantity AS billingQuantity, "
            + "r.unit_price AS unitPrice, r.amount, r.currency, r.source_type AS sourceType, "
            + "r.source_ref AS sourceRef, r.remark, r.create_time AS createTime "
            + "FROM wms_billing_record r "
            + "LEFT JOIN sys_tenant t ON t.id = r.erp_tenant_id "
            + "WHERE r.wms_tenant_id = #{wmsTenantId} AND r.bill_month = #{billMonth} "
            + "AND r.charge_status = 'POSTED' ORDER BY r.create_time, r.id")
    java.util.List<BillingRecordVO> listPostedByBill(
            @Param("wmsTenantId") Long wmsTenantId, @Param("billMonth") String billMonth);
}
