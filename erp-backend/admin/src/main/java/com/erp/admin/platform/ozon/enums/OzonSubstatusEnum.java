package com.erp.admin.platform.ozon.enums;

/**
 * Ozon 平台子状态枚举（platformSubstatus）
 * <p>
 * 来源：Ozon Seller API - FBS/FBO posting substatus
 * code 与 Ozon API 返回的原始值一致（posting_* 前缀格式）
 */
public enum OzonSubstatusEnum {

	// ======== 处理阶段 ========

	/** 发货单已创建 */
	POSTING_CREATED("posting_created", "已创建"),

	/** 正在打包 */
	POSTING_PACKING("posting_packing", "打包中"),

	/** 订单拆分待处理 */
	POSTING_SPLIT_PENDING("posting_split_pending", "拆分待处理"),

	/** 发货单已注册 */
	POSTING_REGISTERED("posting_registered", "已注册"),

	/** 发货单注册错误 */
	POSTING_REGISTRATION_ERROR("posting_registration_error", "注册错误"),

	/** 等待买家护照数据 */
	POSTING_AWAITING_PASSPORT_DATA("posting_awaiting_passport_data", "等待护照数据"),

	/** 正在进行验收 */
	POSTING_ACCEPTANCE_IN_PROGRESS("posting_acceptance_in_progress", "验收中"),

	/** 未被分拣中心接收 */
	POSTING_NOT_IN_SORT_CENTER("posting_not_in_sort_center", "未入分拣中心"),

	// ======== 物流阶段 ========

	/** 正在转交配送服务 */
	POSTING_TRANSFERRING_TO_DELIVERY("posting_transferring_to_delivery", "转交配送中"),

	/** 已转交快递服务 */
	POSTING_TRANSFERRED_TO_COURIER_SERVICE("posting_transferred_to_courier_service", "已转交快递"),

	/** 快递员在路上 */
	POSTING_IN_COURIER_SERVICE("posting_in_courier_service", "快递在途"),

	/** 司机正在取件 */
	POSTING_DRIVER_PICK_UP("posting_driver_pick_up", "司机取件"),

	/** 在运输途中 */
	POSTING_IN_CARRIAGE("posting_in_carriage", "运输中"),

	/** 未被加入运输 */
	POSTING_NOT_IN_CARRIAGE("posting_not_in_carriage", "未加入运输"),

	/** 正在运往目的城市 */
	POSTING_ON_WAY_TO_CITY("posting_on_way_to_city", "运往城市"),

	/** 正在运往自提点 */
	POSTING_ON_WAY_TO_PICKUP_POINT("posting_on_way_to_pickup_point", "运往自提点"),

	/** 已到达自提点 */
	POSTING_IN_PICKUP_POINT("posting_in_pickup_point", "在自提点"),

	// ======== 仲裁 ========

	/** 订单进入仲裁流程 */
	POSTING_IN_ARBITRATION("posting_in_arbitration", "仲裁中"),

	/** 客户发起配送仲裁 */
	POSTING_IN_CLIENT_ARBITRATION("posting_in_client_arbitration", "客户仲裁"),

	// ======== 完成 ========

	/** 已送达 */
	POSTING_DELIVERED("posting_delivered", "已送达"),

	/** 买家已收货 */
	POSTING_RECEIVED("posting_received", "已收货"),

	/** 有条件送达 */
	POSTING_CONDITIONALLY_DELIVERED("posting_conditionally_delivered", "有条件送达"),

	/** 已退回仓库 */
	POSTING_RETURNED_TO_WAREHOUSE("posting_returned_to_warehouse", "已退回仓库"),

	/** 发货单已取消 */
	POSTING_CANCELED("posting_canceled", "已取消"),

	/** 发货失败 */
	SHIP_FAILED("ship_failed", "发货失败");

	private final String code;
	private final String label;

	OzonSubstatusEnum(String code, String label) {
		this.code = code;
		this.label = label;
	}

	public String getCode() {
		return code;
	}

	public String getLabel() {
		return label;
	}

	public static OzonSubstatusEnum fromCode(String code) {
		if (code == null)
			return null;
		String normalized = code.toLowerCase();
		for (OzonSubstatusEnum e : values()) {
			if (e.code.equals(normalized)) {
				return e;
			}
		}
		return null;
	}

	/**
	 * 获取子状态的中文标签
	 *
	 * @param code 子状态代码
	 * @return 中文标签，如果找不到则返回原始代码
	 */
	public static String getLabelByCode(String code) {
		OzonSubstatusEnum substatus = fromCode(code);
		return substatus != null ? substatus.getLabel() : code;
	}
}
