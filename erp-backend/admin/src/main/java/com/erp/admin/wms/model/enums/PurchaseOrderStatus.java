package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 采购单业务状态枚举
 * <p>
 * 仅表达用户操作驱动的业务流程状态
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum PurchaseOrderStatus {

	DRAFT("草稿"),
	CONFIRMED("已确认"),
	IN_PRODUCTION("生产中"),
	COMPLETED("已完成"),
	CANCELLED("已取消");

	private final String description;

	/**
	 * 获取允许流转到的目标状态集合
	 * @return 允许的目标状态集合
	 */
	public Set<PurchaseOrderStatus> getAllowedTransitions() {
		switch (this) {
			case DRAFT:
				return EnumSet.of(CONFIRMED, CANCELLED);
			case CONFIRMED:
				return EnumSet.of(IN_PRODUCTION, CANCELLED);
			case IN_PRODUCTION:
				// IN_PRODUCTION -> COMPLETED 由入库状态自动触发，不允许手动流转
				return Collections.emptySet();
			case COMPLETED:
			case CANCELLED:
				return Collections.emptySet();
			default:
				return Collections.emptySet();
		}
	}

	/**
	 * 判断是否可以流转到目标状态
	 * @param target 目标状态
	 * @return 是否允许流转
	 */
	public boolean canTransitionTo(PurchaseOrderStatus target) {
		return getAllowedTransitions().contains(target);
	}

	/**
	 * 判断是否可以编辑全部字段（仅草稿状态）
	 */
	public boolean canEditAll() {
		return this == DRAFT;
	}

	/**
	 * 判断是否可以编辑部分字段（草稿、已确认、生产中）
	 */
	public boolean canEditPartial() {
		return this == DRAFT || this == CONFIRMED || this == IN_PRODUCTION;
	}

	/**
	 * 判断是否可以删除（仅草稿状态）
	 */
	public boolean canDelete() {
		return this == DRAFT;
	}

	/**
	 * 判断是否可以取消（草稿或已确认状态）
	 */
	public boolean canCancel() {
		return this == DRAFT || this == CONFIRMED;
	}

}
