package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.erp.admin.wms.model.entity.InventoryEvent;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

@InterceptorIgnore(tenantLine = "true")
public interface InventoryEventMapper extends ExtendMapper<InventoryEvent> {
	@Select("SELECT * FROM wms_inventory_event WHERE tenant_id = #{tenantId} AND idempotency_key = #{key} LIMIT 1")
	InventoryEvent selectByIdempotencyKey(@Param("tenantId") Long tenantId, @Param("key") String key);
}
