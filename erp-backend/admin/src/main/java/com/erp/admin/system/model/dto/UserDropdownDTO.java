package com.erp.admin.system.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户下拉框选项DTO
 *
 * @author ballcat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDropdownDTO {

	/**
	 * 用户ID
	 */
	private Long value;

	/**
	 * 用户显示名称
	 */
	private String name;

}
