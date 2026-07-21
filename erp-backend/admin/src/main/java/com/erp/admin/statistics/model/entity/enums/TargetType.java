package com.erp.admin.statistics.model.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 销售目标类型枚举
 *
 * @author erp
 */
@Getter
public enum TargetType {

	/**
	 * 年度目标
	 */
	YEARLY("YEARLY", "年度目标"),

	/**
	 * 月度目标
	 */
	MONTHLY("MONTHLY", "月度目标");

	@EnumValue
	@JsonValue
	private final String value;

	private final String description;

	TargetType(String value, String description) {
		this.value = value;
		this.description = description;
	}

}
