package com.erp.admin.wms.model.enums;

import lombok.Getter;

/**
 * 库位调整单状态。
 *
 * <p>平台新建即 PENDING（待调整，不动库存）→ 点「调整完成」逐条执行移库 → COMPLETED（已完成，留痕）；
 * 执行前可 CANCELLED（已取消）。
 *
 * @author erp
 */
@Getter
public enum LocationTransferStatus {

	/** 待调整（已建单，尚未执行移库） */
	PENDING("PENDING", "待调整"),

	/** 已完成（已按明细逐条移库） */
	COMPLETED("COMPLETED", "已完成"),

	/** 已取消（执行前撤销） */
	CANCELLED("CANCELLED", "已取消");

	private final String code;

	private final String desc;

	LocationTransferStatus(String code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	public static LocationTransferStatus fromCode(String code) {
		for (LocationTransferStatus s : values()) {
			if (s.code.equals(code)) {
				return s;
			}
		}
		return null;
	}

}
