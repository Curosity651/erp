package com.erp.admin.tenant.service;

import com.erp.admin.tenant.enums.TenantResultCode;
import com.erp.admin.tenant.exception.TenantBusinessException;
import com.erp.admin.tenant.mapper.ErpAccountMapper;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import lombok.RequiredArgsConstructor;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 当前登录用户的身份服务（三平台隔离）。
 *
 * <p>三种身份本质是三个相互隔离的后台平台，<b>统一由 {@code tenant_type} 判定</b>（不再按角色码判平台）：
 * <ul>
 * <li>{@code OVERSEAS_PLATFORM}：海外仓平台（单实例，平台运营方）。</li>
 * <li>{@code WMS_OPERATOR}：WMS 服务商。</li>
 * <li>{@code ERP_USER}：货主。</li>
 * </ul>
 *
 * <p>用户归属其实例由 {@code sys_user.tenant_id} 解析；{@code is_admin} 取自 {@code sys_user.is_admin}（仅开通的初始管理员置 1）。
 * 提供「取当前身份」与「登录入口校验」，供前端三入口登录与菜单裁剪使用。
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class TenantIdentityService {

	/** 海外仓平台身份标识（= tenant_type）。 */
	public static final String IDENTITY_OVERSEAS_PLATFORM = "OVERSEAS_PLATFORM";

	/** WMS 服务商身份标识（= tenant_type）。 */
	public static final String IDENTITY_WMS_OPERATOR = "WMS_OPERATOR";

	/** 货主身份标识（= tenant_type）。 */
	public static final String IDENTITY_ERP_USER = "ERP_USER";

	private final PrincipalAttributeAccessor principalAttributeAccessor;

	private final ErpAccountMapper erpAccountMapper;

	private final SysTenantMapper sysTenantMapper;

	/**
	 * 取当前登录用户的身份，并按需校验登录入口。
	 * @param expectType 前端选择的入口类型（OVERSEAS_PLATFORM/WMS_OPERATOR/ERP_USER）；为空则不校验
	 * @return 当前身份
	 * @throws TenantBusinessException 未绑定实例、或入口与身份不匹配
	 */
	public TenantIdentityVO currentIdentity(String expectType) {
		Long userId = principalAttributeAccessor.getUserId();
		if (userId == null) {
			throw new TenantBusinessException(TenantResultCode.USER_TENANT_NOT_BOUND);
		}

		// 三平台隔离：用户归属其实例，统一由 sys_user.tenant_id → tenant_type 判定身份
		Long tenantId = erpAccountMapper.selectTenantIdByUserId(userId);
		if (tenantId == null) {
			throw new TenantBusinessException(TenantResultCode.USER_TENANT_NOT_BOUND);
		}
		SysTenant tenant = sysTenantMapper.selectById(tenantId);
		if (tenant == null) {
			throw new TenantBusinessException(TenantResultCode.USER_TENANT_NOT_BOUND);
		}
		// 租户停用 → fail-closed 拦截登录/身份解析（服务商停用会级联停用其名下货主）
		if (tenant.getStatus() != null && tenant.getStatus() != 1) {
			throw new TenantBusinessException(TenantResultCode.TENANT_DISABLED);
		}
		if (!entryMatches(expectType, tenant.getTenantType())) {
			throw new TenantBusinessException(TenantResultCode.ENTRY_TYPE_MISMATCH);
		}

		Integer isAdmin = erpAccountMapper.selectIsAdminById(userId);

		TenantIdentityVO vo = new TenantIdentityVO();
		vo.setIdentityType(tenant.getTenantType());
		vo.setTenantId(tenant.getId());
		vo.setTenantType(tenant.getTenantType());
		vo.setTenantCode(tenant.getTenantCode());
		vo.setTenantName(tenant.getTenantName());
		vo.setAdmin(isAdmin != null && isAdmin == 1);
		return vo;
	}

	/**
	 * 入口与真实身份是否匹配（纯函数，便于单测）。
	 * @param expectType 前端选择的入口类型；空白视为「不校验」
	 * @param actualType 用户真实身份层级
	 * @return 匹配则 true
	 */
	public static boolean entryMatches(String expectType, String actualType) {
		if (!StringUtils.hasText(expectType)) {
			return true;
		}
		return expectType.equals(actualType);
	}

}
