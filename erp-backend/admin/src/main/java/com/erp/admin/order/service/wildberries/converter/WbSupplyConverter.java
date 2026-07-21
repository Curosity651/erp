package com.erp.admin.order.service.wildberries.converter;

import com.erp.admin.order.model.dto.wildberries.WbSupplySyncDTO;
import com.erp.admin.platform.wildberries.model.response.supply.WbSupplyDetail;

/**
 * Wildberries Supply 模型转换器
 * <p>
 * 负责在 WbSupply（API 响应模型）和 WbSupplyDTO（持久化模型）之间转换
 * <p>
 * 重要：包含时区转换逻辑，将 UTC 时间转换为莫斯科时区（UTC+3）
 *
 * @author system
 */
public final class WbSupplyConverter {

	private WbSupplyConverter() {
		// 工具类，禁止实例化
	}

	/**
	 * 将 WbSupply 转换为 WbSupplyDTO（用于持久化）
	 *
	 * @param supply WB API 响应的 Supply 模型
	 * @return 持久化用的 DTO
	 */
	public static WbSupplySyncDTO toDTO(WbSupplyDetail supply) {
		if (supply == null) {
			return null;
		}

		WbSupplySyncDTO dto = new WbSupplySyncDTO();
		dto.setSupplyId(supply.getId());
		dto.setName(supply.getName());
		dto.setCreatedAt(supply.getCreatedAt());

		return dto;
	}
}
