package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 调整单状态枚举
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum AdjustmentStatus {

	/**
	 * 待货主确认（平台发起报废、已冻结待报废货物，等待货主确认销毁）
	 */
	PENDING_OWNER("PENDING_OWNER", "待货主确认"),

	/**
	 * 货主已同意，等待海外仓执行实际销毁。
	 */
	PENDING_DESTROY("PENDING_DESTROY", "待仓库销毁"),

	/**
	 * 已销毁（海外仓确认实际销毁后真正扣减批次库存）
	 */
	SCRAPPED("SCRAPPED", "已销毁"),

	/**
	 * 已驳回（货主驳回报废申请，冻结释放，货物恢复）
	 */
	REJECTED("REJECTED", "已驳回"),

	/**
	 * 已取消（平台在货主处理前撤销，冻结释放）
	 */
	CANCELLED("CANCELLED", "已取消");

	private final String code;

	private final String name;

	/**
	 * 根据 code 获取枚举
	 * @param code 编码
	 * @return 枚举
	 */
	public static AdjustmentStatus fromCode(String code) {
		for (AdjustmentStatus status : values()) {
			if (status.getCode().equals(code)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Invalid adjustment status: " + code);
	}

}
