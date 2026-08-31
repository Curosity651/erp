package com.erp.admin.platform;

import com.erp.admin.platform.finance.mapper.WmsMonthlyBillMapper;
import com.erp.admin.platform.finance.mapper.WmsBillingRecordMapper;
import com.erp.admin.platform.finance.model.dto.GenerateBillDTO;
import com.erp.admin.platform.finance.model.dto.BillAdjustmentDTO;
import com.erp.admin.platform.finance.model.entity.WmsMonthlyBill;
import com.erp.admin.platform.finance.model.entity.WmsBillingRecord;
import com.erp.admin.platform.finance.model.enums.BillAdjustmentType;
import com.erp.admin.platform.finance.model.vo.FeeAmountRow;
import com.erp.admin.platform.finance.model.vo.GenerateBillResultVO;
import com.erp.admin.platform.finance.service.MonthlyBillService;
import com.erp.admin.platform.finance.service.WarehouseBillingService;
import com.erp.admin.system.model.vo.SysFileVO;
import com.erp.admin.system.service.SysFileService;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import org.ballcat.common.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ballcat.security.core.PrincipalAttributeAccessor;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 应收账单：计费落账(纯逻辑) + 生成/重算跳过 + 状态流转 测试。
 *
 * @author erp
 */
class MonthlyBillServiceTest {

    private WmsMonthlyBillMapper mapper;
    private TenantIdentityService tis;
    private WmsBillingRecordMapper billingRecordMapper;
    private WarehouseBillingService warehouseBillingService;
    private SysFileService sysFileService;
    private PrincipalAttributeAccessor principalAttributeAccessor;
    private MonthlyBillService service;

    @BeforeEach
    void setUp() {
        mapper = mock(WmsMonthlyBillMapper.class);
        billingRecordMapper = mock(WmsBillingRecordMapper.class);
        warehouseBillingService = mock(WarehouseBillingService.class);
        sysFileService = mock(SysFileService.class);
        principalAttributeAccessor = mock(PrincipalAttributeAccessor.class);
        when(principalAttributeAccessor.getUserId()).thenReturn(88L);
        when(principalAttributeAccessor.getUsername()).thenReturn("平台管理员");
        tis = mock(TenantIdentityService.class);
        TenantIdentityVO id = mock(TenantIdentityVO.class);
        when(id.getIdentityType()).thenReturn(TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM);
        when(tis.currentIdentity(any())).thenReturn(id);
        service = new MonthlyBillService(mapper, billingRecordMapper, warehouseBillingService,
                sysFileService, tis, principalAttributeAccessor);
    }

    private FeeAmountRow fee(String type, String amt) {
        FeeAmountRow r = new FeeAmountRow();
        r.setFeeType(type);
        r.setAmount(new BigDecimal(amt));
        return r;
    }

    @Test
    void monthly_bill_excludes_contract_rack_rent() {
        WmsMonthlyBill bill = new WmsMonthlyBill();
        MonthlyBillService.applyFees(bill, new BigDecimal("1000"),
                Arrays.asList(fee("INBOUND", "100"), fee("OUTBOUND", "200"), fee("DRIVER", "50")), null);
        assertThat(bill.getRackFee()).isEqualByComparingTo("0");
        assertThat(bill.getInboundFee()).isEqualByComparingTo("100");
        assertThat(bill.getOutboundFee()).isEqualByComparingTo("200");
        assertThat(bill.getDriverFee()).isEqualByComparingTo("50");
        assertThat(bill.getReturnFee()).isEqualByComparingTo("0");
        // 月账单只收当月服务费，不再重复收合同货架租赁费。
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("350");
    }

    @Test
    void apply_fees_applies_discount_to_operation_only() {
        WmsMonthlyBill bill = new WmsMonthlyBill();
        // 九折：discount = -0.10 → factor 0.90，仅作用操作费；货架租金不打折
        MonthlyBillService.applyFees(bill, new BigDecimal("1000"),
                Collections.singletonList(fee("INBOUND", "200")), new BigDecimal("-0.10"));
        assertThat(bill.getInboundFee()).isEqualByComparingTo("180.00"); // 200×0.9
        assertThat(bill.getRackFee()).isEqualByComparingTo("0");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("180.00");
    }

    @Test
    void bill_adjustment_is_not_discounted_again() {
        WmsMonthlyBill bill = new WmsMonthlyBill();
        FeeAmountRow row = fee("INBOUND", "200");
        row.setAdjustmentAmount(new BigDecimal("20"));

        MonthlyBillService.applyFees(bill, BigDecimal.ZERO,
                Collections.singletonList(row), new BigDecimal("-0.10"));

        assertThat(bill.getInboundFee()).isEqualByComparingTo("200.00");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void generate_skips_confirmed_and_recalcs_draft() {
        // 服务商 5：已确认 → 跳过；服务商 6：草稿 → 重算
        when(mapper.selectWmsOperatorIds()).thenReturn(Arrays.asList(5L, 6L));
        WmsMonthlyBill confirmed = new WmsMonthlyBill();
        confirmed.setStatus("CONFIRMED");
        WmsMonthlyBill draft = new WmsMonthlyBill();
        draft.setStatus("DRAFT");
        when(mapper.selectByMonthAndTenant(eq("2026-06"), eq(5L))).thenReturn(confirmed);
        when(mapper.selectByMonthAndTenant(eq("2026-06"), eq(6L))).thenReturn(draft);
        when(mapper.sumFeeByBill(any())).thenReturn(Collections.emptyList());
        when(mapper.selectDiscountPct(any(), any())).thenReturn(null);

        GenerateBillDTO dto = new GenerateBillDTO();
        dto.setBillMonth("2026-06");
        GenerateBillResultVO r = service.generate(dto);

        assertThat(r.getSkipped()).isEqualTo(1);
        assertThat(r.getRecalculated()).isEqualTo(1);
        assertThat(r.getCreated()).isEqualTo(0);
        verify(mapper).updateById(draft);       // 草稿被重算
        verify(mapper, never()).insert(any());   // 无新建
    }

    @Test
    void generate_creates_when_absent() {
        when(mapper.selectByMonthAndTenant(anyString(), eq(9L))).thenReturn(null);
        when(mapper.sumFeeByBill(any())).thenReturn(Collections.emptyList());
        when(mapper.selectDiscountPct(any(), any())).thenReturn(null);

        GenerateBillDTO dto = new GenerateBillDTO();
        dto.setBillMonth("2026-06");
        dto.setWmsTenantId(9L);
        GenerateBillResultVO r = service.generate(dto);

        assertThat(r.getCreated()).isEqualTo(1);
        org.mockito.ArgumentCaptor<WmsMonthlyBill> billCaptor = org.mockito.ArgumentCaptor
            .forClass(WmsMonthlyBill.class);
        verify(mapper).insert(billCaptor.capture());
        assertThat(billCaptor.getValue().getCurrency()).isEqualTo("CNY");
    }

    @Test
    void pay_requires_confirmed() {
        SysFileVO voucher = new SysFileVO();
        voucher.setContentType("application/pdf");
        when(sysFileService.getFileInfo(10L)).thenReturn(voucher);
        WmsMonthlyBill draft = new WmsMonthlyBill();
        draft.setId(1L);
        draft.setStatus("DRAFT");
        when(mapper.selectById(1L)).thenReturn(draft);
        assertThatThrownBy(() -> service.pay(1L, 10L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void pay_requires_payment_voucher() {
        assertThatThrownBy(() -> service.pay(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("付款凭证");
    }

    @Test
    void confirm_from_draft_ok() {
        WmsMonthlyBill draft = new WmsMonthlyBill();
        draft.setId(1L);
        draft.setBillMonth(YearMonth.now().minusMonths(1).toString());
        draft.setStatus("DRAFT");
        when(mapper.selectById(1L)).thenReturn(draft);
        when(mapper.selectVoById(1L)).thenReturn(new com.erp.admin.platform.finance.model.vo.MonthlyBillVO());
        when(mapper.confirmIfPending(eq(1L), eq(88L), eq("平台管理员"), any())).thenReturn(1);
        service.confirm(1L);
        verify(mapper).confirmIfPending(eq(1L), eq(88L), eq("平台管理员"), any());
    }

    @Test
    void current_month_bill_cannot_be_confirmed_before_month_closes() {
        WmsMonthlyBill draft = new WmsMonthlyBill();
        draft.setId(1L);
        draft.setBillMonth(YearMonth.now().toString());
        draft.setStatus("DRAFT");
        when(mapper.selectById(1L)).thenReturn(draft);

        assertThatThrownBy(() -> service.confirm(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账期尚未结束");
    }

    @Test
    void failed_conditional_status_update_is_reported_as_concurrent_change() {
        WmsMonthlyBill draft = new WmsMonthlyBill();
        draft.setId(1L);
        draft.setBillMonth(YearMonth.now().minusMonths(1).toString());
        draft.setStatus("DRAFT");
        when(mapper.selectById(1L)).thenReturn(draft);
        when(mapper.confirmIfPending(eq(1L), eq(88L), eq("平台管理员"), any())).thenReturn(0);

        assertThatThrownBy(() -> service.confirm(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("状态已变化");
    }

    @Test
    void draft_bill_accepts_adjustment_and_recalculates_total() {
        WmsMonthlyBill draft = new WmsMonthlyBill();
        draft.setId(7L);
        draft.setWmsTenantId(9L);
        draft.setBillMonth("2026-07");
        draft.setCurrency("CNY");
        draft.setStatus("DRAFT");
        when(mapper.selectById(7L)).thenReturn(draft);
        when(mapper.sumFeeByBill(7L)).thenReturn(Collections.singletonList(fee("INBOUND", "-20")));
        when(mapper.selectVoById(7L)).thenReturn(new com.erp.admin.platform.finance.model.vo.MonthlyBillVO());

        BillAdjustmentDTO dto = adjustment(BillAdjustmentType.DEDUCTION, "20");
        service.addAdjustment(7L, dto);

        verify(warehouseBillingService).recordBillAdjustment(draft, dto, 88L, "平台管理员");
        assertThat(draft.getTotalAmount()).isEqualByComparingTo("-20.00");
        verify(mapper).updateById(draft);
    }

    @Test
    void reviewed_bill_rejects_adjustment() {
        WmsMonthlyBill reviewed = new WmsMonthlyBill();
        reviewed.setId(8L);
        reviewed.setStatus("CONFIRMED");
        when(mapper.selectById(8L)).thenReturn(reviewed);

        assertThatThrownBy(() -> service.addAdjustment(8L,
                adjustment(BillAdjustmentType.SUPPLEMENT, "20")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已复核");
        verify(warehouseBillingService, never()).recordBillAdjustment(any(), any(), any(), any());
    }

    private BillAdjustmentDTO adjustment(BillAdjustmentType type, String amount) {
        BillAdjustmentDTO dto = new BillAdjustmentDTO();
        dto.setAdjustmentType(type);
        dto.setFeeCode("INBOUND_CBM");
        dto.setAmount(new BigDecimal(amount));
        dto.setSourceRef("ADJ-001");
        dto.setRemark("修正测试");
        return dto;
    }

}
