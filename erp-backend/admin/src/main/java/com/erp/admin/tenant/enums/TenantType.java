package com.erp.admin.tenant.enums;

/**
 * 租户类型（三平台模型）。
 *
 * <p>三种身份本质是三个相互隔离的后台平台，共用一套代码/库，统一由 {@code tenant_type} 判定：
 * <ul>
 * <li>{@link #OVERSEAS_PLATFORM}：海外仓平台（软件平台 + 物理仓运营方），<b>单实例</b>。管平台资源（仓库/货架/计费）+ 开通服务商。
 * 不是权限超管，对任何实例的用户/角色零可见。</li>
 * <li>{@link #WMS_OPERATOR}：WMS 服务商（3PL 中间人），多实例。开通货主、转卖物流产品。</li>
 * <li>{@link #ERP_USER}：货主（ERP/OMS 使用者），多实例，经营自己的商品/订单/店铺。</li>
 * </ul>
 *
 * <p>存储为 {@code sys_tenant.tenant_type} 字符串（与枚举名一致）。
 *
 * @author erp
 */
public enum TenantType {

	/** 海外仓平台（单实例，平台运营方）。 */
	OVERSEAS_PLATFORM,

	/** WMS 服务商（3PL 中间人）。 */
	WMS_OPERATOR,

	/** 货主 / ERP-OMS 使用者。 */
	ERP_USER;

	/**
	 * 由字符串解析租户类型。
	 * @param code 数据库存储的 tenant_type 值
	 * @return 对应枚举；无法识别（含 null）时返回 null
	 */
	public static TenantType of(String code) {
		if (code == null) {
			return null;
		}
		for (TenantType type : values()) {
			if (type.name().equals(code)) {
				return type;
			}
		}
		return null;
	}

}
