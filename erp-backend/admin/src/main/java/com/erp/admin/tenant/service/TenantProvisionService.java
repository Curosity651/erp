package com.erp.admin.tenant.service;

import java.util.List;
import java.util.stream.Collectors;

import com.erp.admin.tenant.enums.TenantResultCode;
import com.erp.admin.tenant.enums.TenantType;
import com.erp.admin.tenant.exception.TenantBusinessException;
import com.erp.admin.tenant.mapper.ErpAccountMapper;
import com.erp.admin.tenant.mapper.ErpUserRoleMapper;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.mapper.TenantRoleMapper;
import com.erp.admin.tenant.model.dto.OpenTenantDTO;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.tenant.model.entity.SysUserAccount;
import com.erp.admin.tenant.model.entity.SysUserRole;
import com.erp.admin.tenant.model.qo.TenantQO;
import com.erp.admin.tenant.model.vo.TenantBriefVO;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.toolkit.WrappersX;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 双层租户开通服务（B3）。
 *
 * <ul>
 * <li>平台超管 开通 WMS 服务商（{@code WMS_OPERATOR}，parent=NULL）。</li>
 * <li>WMS 服务商 开通 货主（{@code ERP_USER}，parent=当前服务商，禁止跨商）。</li>
 * </ul>
 *
 * 开通同时创建租户 + 管理员账号（{@code ROLE_ADMIN} + is_admin=1）。
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class TenantProvisionService {

	/** 租户管理员角色码前缀；实际为 {@code ROLE_ADMIN_T<tenantId>}，每租户独立（V19 角色租户化）。 */
	private static final String TENANT_ADMIN_ROLE_PREFIX = "ROLE_ADMIN_T";

	private final TenantIdentityService tenantIdentityService;

	private final SysTenantMapper sysTenantMapper;

	private final ErpUserRoleMapper erpUserRoleMapper;

	private final ErpAccountMapper erpAccountMapper;

	private final TenantRoleMapper tenantRoleMapper;

	private final PasswordEncoder passwordEncoder;

	/**
	 * 断言当前登录者为其后台的租户管理员（is_admin=1）。
	 * <p>租户开通/停用属高危写操作,仅管理员可执行;身份类型闸门只保证"对的后台",此断言进一步挡住
	 * 特权后台内的非管理员子账号越权建/停租户。
	 */
	private void assertTenantAdmin() {
		TenantIdentityVO identity = tenantIdentityService.currentIdentity(null);
		if (!Boolean.TRUE.equals(identity.getAdmin())) {
			throw new TenantBusinessException(TenantResultCode.OPEN_TENANT_FORBIDDEN);
		}
	}

	/**
	 * 平台超管开通 WMS 服务商。
	 * @param dto 开通信息
	 * @return 新建服务商租户 ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long openWmsOperator(OpenTenantDTO dto) {
		assertTenantAdmin();
		String identityType = tenantIdentityService.currentIdentity(null).getIdentityType();
		if (!canOpenWmsOperator(identityType)) {
			throw new TenantBusinessException(TenantResultCode.OPEN_TENANT_FORBIDDEN);
		}
		return createTenantWithAdmin(TenantType.WMS_OPERATOR.name(), null, dto);
	}

	/**
	 * WMS 服务商开通货主（强制挂在当前服务商名下）。
	 * @param dto 开通信息
	 * @return 新建货主租户 ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long openErpTenant(OpenTenantDTO dto) {
		assertTenantAdmin();
		TenantIdentityVO identity = tenantIdentityService.currentIdentity(null);
		if (!canOpenErpTenant(identity.getIdentityType())) {
			throw new TenantBusinessException(TenantResultCode.OPEN_TENANT_FORBIDDEN);
		}
		// 服务端强制 parent = 当前登录服务商，杜绝跨商开通
		return createTenantWithAdmin(TenantType.ERP_USER.name(), identity.getTenantId(), dto);
	}

	/**
	 * 列出全部 WMS 服务商（仅平台超管）。
	 * @return 服务商列表
	 */
	public List<TenantBriefVO> listWmsOperators() {
		if (!canOpenWmsOperator(tenantIdentityService.currentIdentity(null).getIdentityType())) {
			throw new TenantBusinessException(TenantResultCode.OPEN_TENANT_FORBIDDEN);
		}
		List<SysTenant> list = sysTenantMapper.selectList(WrappersX.lambdaQueryX(SysTenant.class)
			.eq(SysTenant::getTenantType, TenantType.WMS_OPERATOR.name())
			.orderByDesc(SysTenant::getId));
		return list.stream().map(this::toBrief).collect(Collectors.toList());
	}

	/**
	 * 分页查询 WMS 服务商（仅平台超管）。
	 * @param pageParam 分页参数
	 * @param qo 查询条件（编码/名称/状态）
	 * @return 服务商分页
	 */
	public PageResult<TenantBriefVO> pageWmsOperators(PageParam pageParam, TenantQO qo) {
		if (!canOpenWmsOperator(tenantIdentityService.currentIdentity(null).getIdentityType())) {
			throw new TenantBusinessException(TenantResultCode.OPEN_TENANT_FORBIDDEN);
		}
		PageResult<SysTenant> page = sysTenantMapper.pageWmsOperators(pageParam, qo);
		List<TenantBriefVO> records = page.getRecords().stream().map(this::toBrief).collect(Collectors.toList());
		return new PageResult<>(records, page.getTotal());
	}

	/**
	 * 启用/停用 WMS 服务商（仅平台超管）。停用时级联停用其名下全部货主（需求书 1.1.3）。
	 * @param id 服务商租户ID
	 * @param status 1-启用 / 0-停用
	 */
	@Transactional(rollbackFor = Exception.class)
	public void setOperatorStatus(Long id, Integer status) {
		assertTenantAdmin();
		if (!canOpenWmsOperator(tenantIdentityService.currentIdentity(null).getIdentityType())) {
			throw new TenantBusinessException(TenantResultCode.OPEN_TENANT_FORBIDDEN);
		}
		SysTenant tenant = sysTenantMapper.selectById(id);
		if (tenant == null || !TenantType.WMS_OPERATOR.name().equals(tenant.getTenantType())) {
			throw new TenantBusinessException(TenantResultCode.OPEN_TENANT_FORBIDDEN.getCode(), "服务商不存在");
		}
		tenant.setStatus(status);
		sysTenantMapper.updateById(tenant);
		// 停用服务商：级联停用其名下货主；启用仅启用本身（货主由管理员按需逐个恢复）
		if (status != null && status == 0) {
			SysTenant child = new SysTenant();
			child.setStatus(0);
			sysTenantMapper.update(child, WrappersX.lambdaQueryX(SysTenant.class)
				.eq(SysTenant::getTenantType, TenantType.ERP_USER.name())
				.eq(SysTenant::getParentWmsTenantId, id));
		}
	}

	/**
	 * 查看某 WMS 服务商名下的货主（仅平台超管，用于详情下钻）。
	 * @param wmsTenantId 服务商租户ID
	 * @return 货主列表
	 */
	public List<TenantBriefVO> listErpTenantsByParent(Long wmsTenantId) {
		if (!canOpenWmsOperator(tenantIdentityService.currentIdentity(null).getIdentityType())) {
			throw new TenantBusinessException(TenantResultCode.OPEN_TENANT_FORBIDDEN);
		}
		List<SysTenant> list = sysTenantMapper.selectList(WrappersX.lambdaQueryX(SysTenant.class)
			.eq(SysTenant::getTenantType, TenantType.ERP_USER.name())
			.eq(SysTenant::getParentWmsTenantId, wmsTenantId)
			.orderByDesc(SysTenant::getId));
		return list.stream().map(this::toBrief).collect(Collectors.toList());
	}

	/**
	 * 列出平台下全部货主（仅平台超管，含所属服务商ID，用于报废单等平台侧下拉/筛选）。
	 * @return 全部货主列表
	 */
	public List<TenantBriefVO> listAllErpTenants() {
		if (!canOpenWmsOperator(tenantIdentityService.currentIdentity(null).getIdentityType())) {
			throw new TenantBusinessException(TenantResultCode.OPEN_TENANT_FORBIDDEN);
		}
		List<SysTenant> list = sysTenantMapper.selectList(WrappersX.lambdaQueryX(SysTenant.class)
			.eq(SysTenant::getTenantType, TenantType.ERP_USER.name())
			.orderByDesc(SysTenant::getId));
		return list.stream().map(this::toBrief).collect(Collectors.toList());
	}

	/**
	 * 列出当前 WMS 服务商名下的货主（仅 WMS 服务商）。
	 * @return 货主列表
	 */
	public List<TenantBriefVO> listErpTenants() {
		TenantIdentityVO identity = tenantIdentityService.currentIdentity(null);
		if (!canOpenErpTenant(identity.getIdentityType())) {
			throw new TenantBusinessException(TenantResultCode.OPEN_TENANT_FORBIDDEN);
		}
		List<SysTenant> list = sysTenantMapper.selectList(WrappersX.lambdaQueryX(SysTenant.class)
			.eq(SysTenant::getTenantType, TenantType.ERP_USER.name())
			.eq(SysTenant::getParentWmsTenantId, identity.getTenantId())
			.orderByDesc(SysTenant::getId));
		return list.stream().map(this::toBrief).collect(Collectors.toList());
	}

	/**
	 * 分页查询当前 WMS 服务商名下的货主（仅 WMS 服务商）。
	 * @param pageParam 分页参数
	 * @param qo 查询条件（编码/名称/状态）
	 * @return 货主分页
	 */
	public PageResult<TenantBriefVO> pageErpTenants(PageParam pageParam, TenantQO qo) {
		TenantIdentityVO identity = tenantIdentityService.currentIdentity(null);
		if (!canOpenErpTenant(identity.getIdentityType())) {
			throw new TenantBusinessException(TenantResultCode.OPEN_TENANT_FORBIDDEN);
		}
		PageResult<SysTenant> page = sysTenantMapper.pageErpTenants(pageParam, qo, identity.getTenantId());
		List<TenantBriefVO> records = page.getRecords().stream().map(this::toBrief).collect(Collectors.toList());
		return new PageResult<>(records, page.getTotal());
	}

	/**
	 * 启用/停用货主（仅 WMS 服务商，且只能操作自己名下的货主，杜绝跨商）。仅控制账号开通/停用，不删租户。
	 * @param id 货主租户ID
	 * @param status 1-启用 / 0-停用
	 */
	@Transactional(rollbackFor = Exception.class)
	public void setErpTenantStatus(Long id, Integer status) {
		assertTenantAdmin();
		TenantIdentityVO identity = tenantIdentityService.currentIdentity(null);
		if (!canOpenErpTenant(identity.getIdentityType())) {
			throw new TenantBusinessException(TenantResultCode.OPEN_TENANT_FORBIDDEN);
		}
		SysTenant tenant = sysTenantMapper.selectById(id);
		if (tenant == null || !TenantType.ERP_USER.name().equals(tenant.getTenantType())
				|| !identity.getTenantId().equals(tenant.getParentWmsTenantId())) {
			throw new TenantBusinessException(TenantResultCode.OPEN_TENANT_FORBIDDEN.getCode(), "货主不存在或无权操作");
		}
		tenant.setStatus(status);
		sysTenantMapper.updateById(tenant);
	}

	private Long createTenantWithAdmin(String tenantType, Long parentWmsTenantId, OpenTenantDTO dto) {
		validate(dto);
		if (existsTenantCode(dto.getTenantCode())) {
			throw new TenantBusinessException(TenantResultCode.TENANT_CODE_EXISTS);
		}
		if (erpAccountMapper.existsByUsername(dto.getAdminUsername())) {
			throw new TenantBusinessException(TenantResultCode.USERNAME_EXISTS);
		}

		// 1) 租户
		SysTenant tenant = new SysTenant();
		tenant.setTenantCode(dto.getTenantCode());
		tenant.setTenantName(dto.getTenantName());
		tenant.setTenantType(tenantType);
		tenant.setParentWmsTenantId(parentWmsTenantId);
		tenant.setContactName(dto.getContactName());
		tenant.setContactPhone(dto.getContactPhone());
		tenant.setContactEmail(dto.getContactEmail());
		tenant.setStatus(1);
		tenant.setRemark(dto.getRemark());
		sysTenantMapper.insert(tenant);

		// 2) 管理员账号（显式写 tenant_id=新实例：sys_user 已纳入租户白名单，列已存在故不会被回填成开通者的实例）
		SysUserAccount admin = new SysUserAccount();
		admin.setUsername(dto.getAdminUsername());
		admin.setNickname(StringUtils.hasText(dto.getAdminNickname()) ? dto.getAdminNickname() : dto.getTenantName());
		admin.setPassword(passwordEncoder.encode(dto.getAdminPassword()));
		admin.setStatus(1);
		admin.setType(1);
		admin.setTenantId(tenant.getId());
		admin.setIsAdmin(1); // 实例管理员标记并入 sys_user（废弃 sys_user_tenant）
		erpAccountMapper.insert(admin);

		// 3) 铸造该租户<b>独立</b>的管理员角色（V19 角色租户化，不再共享 ROLE_ADMIN），
		//    按身份类型授予允许的菜单集（与 menu-filter.ts / V19 一致），最后绑定管理员用户。
		String roleCode = TENANT_ADMIN_ROLE_PREFIX + tenant.getId();
		tenantRoleMapper.insertTenantAdminRole(roleCode, "管理员", tenant.getId(), tenant.getTenantName() + " 管理员");
		List<Long> menuIds = tenantRoleMapper.selectAllowedMenuIds(tenantType);
		if (!menuIds.isEmpty()) {
			tenantRoleMapper.insertRoleMenus(roleCode, menuIds);
		}

		SysUserRole userRole = new SysUserRole();
		userRole.setUserId(admin.getUserId());
		userRole.setRoleCode(roleCode);
		erpUserRoleMapper.insert(userRole);

		return tenant.getId();
	}

	private boolean existsTenantCode(String tenantCode) {
		return sysTenantMapper
			.selectCount(WrappersX.lambdaQueryX(SysTenant.class).eq(SysTenant::getTenantCode, tenantCode)) > 0;
	}

	private TenantBriefVO toBrief(SysTenant t) {
		TenantBriefVO vo = new TenantBriefVO();
		vo.setId(t.getId());
		vo.setTenantCode(t.getTenantCode());
		vo.setTenantName(t.getTenantName());
		vo.setTenantType(t.getTenantType());
		vo.setParentWmsTenantId(t.getParentWmsTenantId());
		vo.setStatus(t.getStatus());
		vo.setContactName(t.getContactName());
		vo.setContactPhone(t.getContactPhone());
		vo.setCreateTime(t.getCreateTime());
		return vo;
	}

	private static void validate(OpenTenantDTO dto) {
		if (dto == null || !StringUtils.hasText(dto.getTenantCode()) || !StringUtils.hasText(dto.getTenantName())
				|| !StringUtils.hasText(dto.getAdminUsername()) || !StringUtils.hasText(dto.getAdminPassword())) {
			throw new TenantBusinessException(TenantResultCode.OPEN_TENANT_FORBIDDEN.getCode(),
					"租户编码/名称、管理员用户名/密码不能为空");
		}
	}

	/** 海外仓平台才能开 WMS 服务商（纯函数，便于单测）。 */
	public static boolean canOpenWmsOperator(String identityType) {
		return TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(identityType);
	}

	/** WMS 服务商才能开货主（纯函数，便于单测）。 */
	public static boolean canOpenErpTenant(String identityType) {
		return TenantType.WMS_OPERATOR.name().equals(identityType);
	}

}
