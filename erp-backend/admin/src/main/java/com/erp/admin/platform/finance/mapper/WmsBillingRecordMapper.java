package com.erp.admin.platform.finance.mapper;

import com.erp.admin.platform.finance.model.entity.WmsBillingRecord;
import com.erp.admin.platform.finance.model.vo.BillingRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

@Mapper
public interface WmsBillingRecordMapper extends ExtendMapper<WmsBillingRecord> {

    default WmsBillingRecord selectByBizId(String bizId) {
        return selectOne(WrappersX.lambdaQueryX(WmsBillingRecord.class)
                .eq(WmsBillingRecord::getBizId, bizId)
                .last("LIMIT 1"));
    }

    @Update("UPDATE wms_billing_record SET monthly_bill_id = #{billId} "
            + "WHERE monthly_bill_id IS NULL AND wms_tenant_id = #{wmsTenantId} "
            + "AND bill_month = #{billMonth} AND currency = #{currency} "
            + "AND charge_status = 'POSTED'")
    int bindPostedRecordsToBill(@Param("billId") Long billId,
            @Param("wmsTenantId") Long wmsTenantId, @Param("billMonth") String billMonth,
            @Param("currency") String currency);

    @Select("SELECT r.id, r.erp_tenant_id AS erpTenantId, t.tenant_name AS ownerName, "
            + "r.warehouse_id AS warehouseId, r.fee_type AS feeType, r.fee_code AS feeCode, "
            + "COALESCE(NULLIF(SUBSTRING_INDEX(r.rate_snapshot, '|', 1), ''), r.fee_code) AS feeName, "
            + "r.billing_unit AS billingUnit, r.billing_quantity AS billingQuantity, "
            + "r.unit_price AS unitPrice, r.amount, r.currency, r.source_type AS sourceType, "
            + "r.source_ref AS sourceRef, r.remark, r.create_time AS createTime "
            + ", r.operator_id AS operatorId, r.operator_name AS operatorName "
            + "FROM wms_billing_record r "
            + "LEFT JOIN sys_tenant t ON t.id = r.erp_tenant_id "
            + "WHERE r.monthly_bill_id = #{billId} "
            + "AND r.charge_status = 'POSTED' ORDER BY r.create_time, r.id")
    java.util.List<BillingRecordVO> listPostedByBill(@Param("billId") Long billId);
}
