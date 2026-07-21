package com.erp.admin.order.service.wildberries.converter;

import com.erp.admin.order.model.dto.wildberries.WbOrderSyncDTO;
import com.erp.admin.platform.wildberries.model.response.order.WbOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Wildberries 订单模型转换器
 * <p>
 * 负责在 WbOrder（API 响应模型）和 WbOrderDTO（持久化模型）之间转换
 * <p>
 * 时间字段说明：当前实现按平台返回的时间直接落库，不做时区转换；
 * 如需展示其它时区，请在展示层（VO/前端）统一处理。后续若改为统一存 UTC，将另行迁移。
 * 
 * @author system
 */
public final class WbOrderConverter {

	private WbOrderConverter() {
		// 工具类，禁止实例化
	}

    /**
     * 将 WbOrder 转换为 WbOrderDTO（用于持久化）
     * <p>
     * 注意：本实现不做时区转换，按平台时间原样存储
     * 
     * @param order        WB API 响应的订单模型
     * @param objectMapper JSON 序列化器（用于保存原始 JSON）
     * @return 持久化用的 DTO
     */
    public static WbOrderSyncDTO toDTO(WbOrder order, ObjectMapper objectMapper) {
        if (order == null) {
            return null;
        }
        
        WbOrderSyncDTO dto = new WbOrderSyncDTO();
        dto.setOrderId(order.getId() != null ? order.getId().toString() : null);
        dto.setSupplyId(order.getSupplyId());
        dto.setWarehouseId(order.getWarehouseId() != null ? order.getWarehouseId().toString() : null);
        dto.setOfficeId(order.getOfficeId() != null ? order.getOfficeId().toString() : null);
        dto.setArticle(order.getArticle());
        dto.setDeliveryType(order.getDeliveryType());
        dto.setPrice(order.getPrice());
        dto.setConvertedPrice(order.getConvertedPrice());
        dto.setCurrencyCode(order.getCurrencyCode() != null ? order.getCurrencyCode().toString() : null);
        dto.setConvertedCurrencyCode(order.getConvertedCurrencyCode() != null ? order.getConvertedCurrencyCode().toString() : null);
		dto.setComment(order.getComment());
        // 保存原始时间，查询时进行转换处理
        dto.setCreatedAt(order.getCreatedAt());

		// 序列化原始 JSON
		String rawJson;
		try {
			rawJson = objectMapper.writeValueAsString(order);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		dto.setRawJson(rawJson);
        
        return dto;
    }
    
}
