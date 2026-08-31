package com.erp.admin.platform.finance.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.platform.finance.model.entity.WmsMonthlyBill;
import com.erp.admin.platform.finance.model.qo.MonthlyBillQO;
import com.erp.admin.platform.finance.model.vo.FeeAmountRow;
import com.erp.admin.platform.finance.model.vo.MonthlyBillVO;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;

/**
 * 月度应收账单 Mapper：账单头 CRUD + 计费聚合（货架租金/操作费/折扣/服务商列表）。
 *
 * @author erp
 */
public interface WmsMonthlyBillMapper extends ExtendMapper<WmsMonthlyBill> {

    IPage<MonthlyBillVO> pageBills(IPage<MonthlyBillVO> page, @Param("qo") MonthlyBillQO qo);

    MonthlyBillVO selectVoById(@Param("id") Long id);

    /** 某服务商某账期账单（判重/重算用） */
    default WmsMonthlyBill selectByMonthAndTenant(String billMonth, Long wmsTenantId) {
        return this.selectOne(WrappersX.lambdaQueryX(WmsMonthlyBill.class)
            .eq(WmsMonthlyBill::getBillMonth, billMonth)
            .eq(WmsMonthlyBill::getWmsTenantId, wmsTenantId)
            .last("LIMIT 1"));
    }

    /** 货架租金 = Σ 当月有效 wms_rack_assignment.monthly_fee */
    BigDecimal sumRackFee(@Param("wmsTenantId") Long wmsTenantId, @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);

    /** 操作费按类型汇总（wms_billing_record） */
    List<FeeAmountRow> sumFeeByType(@Param("wmsTenantId") Long wmsTenantId, @Param("billMonth") String billMonth);

    List<FeeAmountRow> sumFeeByBill(@Param("billId") Long billId);

    /** 全部 WMS 服务商 ID */
    List<Long> selectWmsOperatorIds();

    /** 服务商费率折扣百分比（无则返回 null） */
    BigDecimal selectDiscountPct(@Param("wmsTenantId") Long wmsTenantId,
            @Param("billingDate") LocalDate billingDate);

    @Update("UPDATE wms_monthly_bill SET status='CONFIRMED', confirmed_time=#{time}, "
            + "reviewer_id=#{reviewerId}, reviewer_name=#{reviewerName}, remark=NULL "
            + "WHERE id=#{id} AND status IN ('DRAFT','DISPUTED') AND deleted=0")
    int confirmIfPending(@Param("id") Long id, @Param("reviewerId") Long reviewerId,
            @Param("reviewerName") String reviewerName, @Param("time") LocalDateTime time);

    @Update("UPDATE wms_monthly_bill SET status='PAID', paid_time=#{time}, "
            + "payment_voucher_file_id=#{voucherId} "
            + "WHERE id=#{id} AND status='CONFIRMED' AND deleted=0")
    int payIfConfirmed(@Param("id") Long id, @Param("voucherId") Long voucherId,
            @Param("time") LocalDateTime time);

    @Update("UPDATE wms_monthly_bill SET status='DISPUTED', remark=#{remark} "
            + "WHERE id=#{id} AND status='CONFIRMED' AND deleted=0")
    int disputeIfConfirmed(@Param("id") Long id, @Param("remark") String remark);

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM wms_monthly_bill "
            + "WHERE wms_tenant_id = #{wmsTenantId} AND status <> 'PAID' "
            + "AND total_amount > 0 AND deleted = 0")
    BigDecimal sumUnpaidAmount(@Param("wmsTenantId") Long wmsTenantId);

}
