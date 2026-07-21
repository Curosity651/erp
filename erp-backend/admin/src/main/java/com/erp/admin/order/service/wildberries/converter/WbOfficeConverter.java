package com.erp.admin.order.service.wildberries.converter;

import java.math.BigDecimal;

import com.erp.admin.order.model.entity.WbOffice;

/**
 * Wildberries Office 模型转换器
 * <p>
 * 负责在 WbOffice（API 响应模型）和 ErpWbOffice（数据库实体）之间转换
 *
 * @author system
 */
public final class WbOfficeConverter {

	private WbOfficeConverter() {
		// 工具类，禁止实例化
	}

	/**
	 * 将 WbOffice 转换为 ErpWbOffice 实体（用于持久化）
	 *
	 * @param office WB API 响应的 Office 模型
	 * @param shopId 店铺ID
	 * @return 数据库实体
	 */
	public static WbOffice toEntity(com.erp.admin.platform.wildberries.model.response.office.WbOffice office, Long shopId) {
		if (office == null) {
			return null;
		}

		WbOffice entity = new WbOffice();
		entity.setShopId(shopId);
		entity.setOfficeId(office.getId());
		entity.setName(office.getName());
		entity.setCity(office.getCity());
		entity.setAddress(office.getAddress());
		entity.setLongitude(office.getLongitude() != null ? BigDecimal.valueOf(office.getLongitude()) : null);
		entity.setLatitude(office.getLatitude() != null ? BigDecimal.valueOf(office.getLatitude()) : null);
		entity.setCargoType(office.getCargoType());
		entity.setDeliveryType(office.getDeliveryType());
		entity.setFederalDistrict(office.getFederalDistrict());
		entity.setSelected(office.getSelected() != null && office.getSelected() ? 1 : 0);

		return entity;
	}

}
