package com.erp.admin.platform;

import com.erp.admin.platform.finance.mapper.WmsMonthlyBillMapper;
import com.erp.admin.platform.finance.mapper.WmsBillingRecordMapper;
import com.erp.admin.platform.finance.model.dto.GenerateBillDTO;
import com.erp.admin.platform.finance.model.entity.WmsMonthlyBill;
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

import java.math.BigDecimal;
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
    private MonthlyBillService service;

    @BeforeEach
    void setUp() {
        mapper = mock(WmsMonthlyBillMapper.class);
        billingRecordMapper = mock(WmsBillingRecordMapper.class);
        warehouseBillingService = mock(WarehouseBillingService.class);
        sysFileService = mock(SysFileService.class);
        tis = mock(TenantIdentityService.class);
        TenantIdentityVO id = mock(TenantIdentityVO.class);
        when(id.getIdentityType()).thenReturn(TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM);
        when(tis.currentIdentity(any())).thenReturn(id);
        service = new MonthlyBillService(mapper, billingRecordMapper, warehouseBillingService, sysFileService, tis);
    }

    private FeeAmountRow fee(String type, String amt) {
        FeeAmountRow r = new FeeAmountRow();
        r.setFeeType(type);
        r.setAmount(new BigDecimal(amt));
        return r;
    }

    @Test
    void apply_fees_maps_categories_and_totals() {
        WmsMonthlyBill bill = new WmsMonthlyBill();
        MonthlyBillService.applyFees(bill, new BigDecimal("1000"),
                Arrays.asList(fee("INBOUND", "100"), fee("OUTBOUND", "200"), fee("DRIVER", "50")), null);
        assertThat(bill.getRackFee()).isEqualByComparingTo("1000");
        assertThat(bill.getInboundFee()).isEqualByComparingTo("100");
        assertThat(bill.getOutboundFee()).isEqualByComparingTo("200");
        assertThat(bill.getDriverFee()).isEqualByComparingTo("50");
        assertThat(bill.getReturnFee()).isEqualByComparingTo("0");
        // 合计 = 1000 + 100 + 200 + 50 = 1350
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("1350");
    }

    @Test
    void apply_fees_applies_discount_to_operation_only() {
        WmsMonthlyBill bill = new WmsMonthlyBill();
        // 九折：discount = -0.10 → factor 0.90，仅作用操作费；货架租金不打折
        MonthlyBillService.applyFees(bill, new BigDecimal("1000"),
                Collections.singletonList(fee("INBOUND", "200")), new BigDecimal("-0.10"));
        assertThat(bill.getInboundFee()).isEqualByComparingTo("180.00"); // 200×0.9
        assertThat(bill.getRackFee()).isEqualByComparingTo("1000");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("1180.00");
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
        when(mapper.sumRackFee(any(), any(), any())).thenReturn(new BigDecimal("100"));
        when(mapper.sumFeeByType(any(), anyString())).thenReturn(Collections.emptyList());
        when(mapper.selectDiscountPct(any())).thenReturn(null);

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
        when(mapper.sumRackFee(any(), any(), any())).thenReturn(new BigDecimal("500"));
        when(mapper.sumFeeByType(any(), anyString())).thenReturn(Collections.emptyList());
        when(mapper.selectDiscountPct(any())).thenReturn(null);

        GenerateBillDTO dto = new GenerateBillDTO();
        dto.setBillMonth("2026-06");
        dto.setWmsTenantId(9L);
        GenerateBillResultVO r = service.generate(dto);

        assertThat(r.getCreated()).isEqualTo(1);
        verify(mapper).insert(any(WmsMonthlyBill.class));
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
        draft.setStatus("DRAFT");
        when(mapper.selectById(1L)).thenReturn(draft);
        when(mapper.selectVoById(1L)).thenReturn(new com.erp.admin.platform.finance.model.vo.MonthlyBillVO());
        service.confirm(1L);
        assertThat(draft.getStatus()).isEqualTo("CONFIRMED");
        assertThat(draft.getConfirmedTime()).isNotNull();
    }

}
