package com.erp.admin.finance.settlement.service;

import com.erp.admin.common.tenant.WmsTenantContext;
import com.erp.admin.finance.settlement.mapper.WmsRechargeOrderMapper;
import com.erp.admin.finance.settlement.model.dto.CreateRechargeDTO;
import com.erp.admin.finance.settlement.model.dto.ReviewRechargeDTO;
import com.erp.admin.finance.settlement.model.dto.ReverseRechargeDTO;
import com.erp.admin.finance.settlement.model.entity.WmsRechargeOrder;
import com.erp.admin.finance.settlement.model.enums.RechargeAccountScope;
import com.erp.admin.finance.settlement.model.enums.RechargeStatus;
import com.erp.admin.system.model.vo.SysFileVO;
import com.erp.admin.system.service.SysFileService;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class RechargeOrderService {

    private static final DateTimeFormatter NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final WmsRechargeOrderMapper rechargeOrderMapper;
    private final SysTenantMapper tenantMapper;
    private final SysFileService sysFileService;
    private final TenantIdentityService tenantIdentityService;
    private final PrincipalAttributeAccessor principalAttributeAccessor;

    @Transactional(rollbackFor = Exception.class)
    public WmsRechargeOrder createOwnerRecharge(CreateRechargeDTO dto) {
        TenantIdentityVO identity = requireIdentity(TenantIdentityService.IDENTITY_ERP_USER);
        SysTenant owner = tenantMapper.selectById(identity.getTenantId());
        Assert.notNull(owner, "货主不存在");
        Assert.notNull(owner.getParentWmsTenantId(), "货主尚未绑定WMS服务商");
        return create(dto, RechargeAccountScope.OWNER_ACCOUNT, owner.getParentWmsTenantId(), owner.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public WmsRechargeOrder createPlatformRecharge(CreateRechargeDTO dto) {
        TenantIdentityVO identity = requireIdentity(TenantIdentityService.IDENTITY_WMS_OPERATOR);
        Long wmsTenantId = WmsTenantContext.getCurrentWmsTenant();
        if (wmsTenantId == null) {
            wmsTenantId = identity.getTenantId();
        }
        return create(dto, RechargeAccountScope.PLATFORM_ACCOUNT, wmsTenantId, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewOwnerRecharge(long id, ReviewRechargeDTO dto) {
        TenantIdentityVO identity = requireIdentity(TenantIdentityService.IDENTITY_WMS_OPERATOR);
        WmsRechargeOrder order = load(id, RechargeAccountScope.OWNER_ACCOUNT);
        Long currentWms = WmsTenantContext.getCurrentWmsTenant();
        if (currentWms == null) currentWms = identity.getTenantId();
        require(currentWms.equals(order.getWmsTenantId()), 403, "无权审核其他服务商的充值单");
        review(order, dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewPlatformRecharge(long id, ReviewRechargeDTO dto) {
        requireIdentity(TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM);
        review(load(id, RechargeAccountScope.PLATFORM_ACCOUNT), dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(long id) {
        WmsRechargeOrder order = rechargeOrderMapper.selectById(id);
        Assert.notNull(order, "充值单不存在");
        Long userId = principalAttributeAccessor.getUserId();
        require(userId != null && userId.equals(order.getApplicantId()), 403, "只能取消本人提交的充值单");
        int changed = rechargeOrderMapper.transition(id, RechargeStatus.PENDING.name(),
                RechargeStatus.CANCELLED.name(), userId, username(), LocalDateTime.now(), "申请人取消");
        require(changed == 1, 409, "充值单状态已变化，请刷新后重试");
    }

    @Transactional(rollbackFor = Exception.class)
    public void reverse(long id, ReverseRechargeDTO dto) {
        WmsRechargeOrder original = rechargeOrderMapper.selectById(id);
        Assert.notNull(original, "充值单不存在");
        authorizeReceiver(original);
        require(RechargeStatus.APPROVED.name().equals(original.getStatus()), 400, "仅已通过充值可以冲正");

        WmsRechargeOrder audit = new WmsRechargeOrder();
        audit.setRechargeNo(nextNo("CZC"));
        audit.setAccountScope(original.getAccountScope());
        audit.setWmsTenantId(original.getWmsTenantId());
        audit.setErpTenantId(original.getErpTenantId());
        audit.setAmount(original.getAmount());
        audit.setCurrency(original.getCurrency());
        audit.setPaymentTime(LocalDateTime.now());
        audit.setVoucherFileId(original.getVoucherFileId());
        audit.setStatus(RechargeStatus.REVERSED.name());
        audit.setApplicantId(principalAttributeAccessor.getUserId());
        audit.setApplicantName(username());
        audit.setReverseOrderId(original.getId());
        audit.setReverseReason(dto.getReason());
        rechargeOrderMapper.insert(audit);

        int changed = rechargeOrderMapper.reverseApproved(original.getId(), audit.getId(),
                principalAttributeAccessor.getUserId(), username(), LocalDateTime.now(), dto.getReason());
        require(changed == 1, 409, "充值单状态已变化，请刷新后重试");
    }

    private WmsRechargeOrder create(CreateRechargeDTO dto, RechargeAccountScope scope,
            Long wmsTenantId, Long erpTenantId) {
        validateVoucher(dto.getVoucherFileId());
        WmsRechargeOrder order = new WmsRechargeOrder();
        order.setRechargeNo(nextNo("CZ"));
        order.setAccountScope(scope.name());
        order.setWmsTenantId(wmsTenantId);
        order.setErpTenantId(erpTenantId);
        order.setAmount(dto.getAmount());
        order.setCurrency(dto.getCurrency().trim().toUpperCase(Locale.ROOT));
        order.setPaymentTime(dto.getPaymentTime());
        order.setVoucherFileId(dto.getVoucherFileId());
        order.setStatus(RechargeStatus.PENDING.name());
        order.setApplicantId(principalAttributeAccessor.getUserId());
        order.setApplicantName(username());
        order.setRemark(dto.getRemark());
        rechargeOrderMapper.insert(order);
        return order;
    }

    private void review(WmsRechargeOrder order, ReviewRechargeDTO dto) {
        if (!Boolean.TRUE.equals(dto.getApproved()) && !StringUtils.hasText(dto.getReason())) {
            throw new BusinessException(400, "驳回时必须填写原因");
        }
        String target = Boolean.TRUE.equals(dto.getApproved())
                ? RechargeStatus.APPROVED.name() : RechargeStatus.REJECTED.name();
        int changed = rechargeOrderMapper.transition(order.getId(), RechargeStatus.PENDING.name(), target,
                principalAttributeAccessor.getUserId(), username(), LocalDateTime.now(), dto.getReason());
        require(changed == 1, 409, "充值单状态已变化，请刷新后重试");
    }

    private void authorizeReceiver(WmsRechargeOrder order) {
        if (RechargeAccountScope.OWNER_ACCOUNT.name().equals(order.getAccountScope())) {
            TenantIdentityVO identity = requireIdentity(TenantIdentityService.IDENTITY_WMS_OPERATOR);
            Long currentWms = WmsTenantContext.getCurrentWmsTenant();
            if (currentWms == null) currentWms = identity.getTenantId();
            require(currentWms.equals(order.getWmsTenantId()), 403, "无权冲正其他服务商的充值单");
        } else {
            requireIdentity(TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM);
        }
    }

    private WmsRechargeOrder load(long id, RechargeAccountScope scope) {
        WmsRechargeOrder order = rechargeOrderMapper.selectById(id);
        Assert.notNull(order, "充值单不存在");
        require(scope.name().equals(order.getAccountScope()), 400, "充值账户类型不匹配");
        return order;
    }

    private TenantIdentityVO requireIdentity(String type) {
        TenantIdentityVO identity = tenantIdentityService.currentIdentity(null);
        require(type.equals(identity.getIdentityType()), 403, "当前身份无权执行此操作");
        return identity;
    }

    private void validateVoucher(Long fileId) {
        SysFileVO file = sysFileService.getFileInfo(fileId);
        Assert.notNull(file, "付款凭证文件不存在，请重新上传");
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!"application/pdf".equals(type) && !"image/jpeg".equals(type)
                && !"image/jpg".equals(type) && !"image/png".equals(type)) {
            throw new BusinessException(400, "付款凭证仅支持 PDF、JPG、PNG 格式");
        }
    }

    private String username() {
        String name = principalAttributeAccessor.getUsername();
        return StringUtils.hasText(name) ? name : "未知用户";
    }

    private static String nextNo(String prefix) {
        return prefix + LocalDateTime.now().format(NO_TIME)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private static void require(boolean condition, int code, String message) {
        if (!condition) throw new BusinessException(code, message);
    }
}
