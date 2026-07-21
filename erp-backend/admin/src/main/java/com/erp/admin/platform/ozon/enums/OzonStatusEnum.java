package com.erp.admin.platform.ozon.enums;

/**
 * Ozon 平台主状态枚举（platformStatus）
 * 来源：Ozon Seller API 货件状态（/v3/posting/fbs/unfulfilled/list 文档列出的官方全集 + delivered）
 */
public enum OzonStatusEnum {

	/** 等待注册（备货前过渡态） */
	AWAITING_REGISTRATION("awaiting_registration", "等待注册"),

	/** 验收中（备货前过渡态） */
	ACCEPTANCE_IN_PROGRESS("acceptance_in_progress", "验收中"),

	/** 等待确认（备货前过渡态） */
	AWAITING_APPROVE("awaiting_approve", "等待确认"),

	/** 等待打包（=运营的"等待备货"） */
	AWAITING_PACKAGING("awaiting_packaging", "等待打包"),

	/** 等待发货（=运营的"等待发运"，确认后落点） */
	AWAITING_DELIVER("awaiting_deliver", "等待发货"),

	/** 司机取货中 */
	DRIVER_PICKUP("driver_pickup", "司机取货中"),

	/** 由卖家发送（跨境头程） */
	SENT_BY_SELLER("sent_by_seller", "卖家已发出"),

	/** 配送中 */
	DELIVERING("delivering", "配送中"),

	/** 已送达 */
	DELIVERED("delivered", "已送达"),

	/** 已取消 */
	CANCELLED("cancelled", "已取消"),

	/** 分拣中心拒收（需人工介入） */
	NOT_ACCEPTED("not_accepted", "分拣中心拒收"),

	/** 仲裁中（需人工介入） */
	ARBITRATION("arbitration", "仲裁中"),

	/** 快递客户仲裁（需人工介入） */
	CLIENT_ARBITRATION("client_arbitration", "客户仲裁");

	private final String code;
	private final String label;

	OzonStatusEnum(String code, String label) {
		this.code = code;
		this.label = label;
	}

	public String getCode() {
		return code;
	}

	public String getLabel() {
		return label;
	}

	public static OzonStatusEnum fromCode(String code) {
		if (code == null)
			return null;
		String normalized = code.toLowerCase();
		for (OzonStatusEnum e : values()) {
			if (e.code.equals(normalized)) {
				return e;
			}
		}
		return null;
	}

	/**
	 * 获取状态的中文标签
	 *
	 * @param code 状态代码
	 * @return 中文标签，如果找不到则返回原始代码
	 */
	public static String getLabelByCode(String code) {
		OzonStatusEnum status = fromCode(code);
		return status != null ? status.getLabel() : code;
	}
}
