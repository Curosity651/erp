package com.erp.admin.wms.service;

import com.erp.admin.wms.mapper.InventoryEventLineMapper;
import com.erp.admin.wms.mapper.InventoryEventMapper;
import com.erp.admin.wms.model.dto.InventoryMutationContext;
import com.erp.admin.wms.model.dto.InventoryMutationLine;
import com.erp.admin.wms.model.entity.InventoryEvent;
import com.erp.admin.wms.model.entity.InventoryEventLine;
import com.erp.admin.wms.model.enums.LocationInventoryQuality;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryEventService {
	private static final DateTimeFormatter NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
	private final InventoryEventMapper eventMapper;
	private final InventoryEventLineMapper lineMapper;

	public boolean exists(Long tenantId, String idempotencyKey) {
		Assert.notNull(tenantId, "平台租户不能为空");
		Assert.hasText(idempotencyKey, "库存事件幂等键不能为空");
		return eventMapper.selectByIdempotencyKey(tenantId, idempotencyKey) != null;
	}

	public InventoryEvent append(InventoryMutationContext context, Long tenantId, Long wmsTenantId,
			Long erpTenantId, Long warehouseId, List<InventoryMutationLine> lines) {
		Assert.notNull(context, "库存事件上下文不能为空");
		Assert.notNull(context.getEventType(), "库存事件类型不能为空");
		Assert.hasText(context.getIdempotencyKey(), "库存事件幂等键不能为空");
		Assert.notEmpty(lines, "库存事件明细不能为空");
		Assert.isTrue(!exists(tenantId, context.getIdempotencyKey()), "库存事件已处理，请勿重复提交");

		LocalDateTime now = LocalDateTime.now();
		InventoryEvent event = new InventoryEvent();
		event.setEventNo("IE" + NO_TIME.format(now) + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
		event.setTenantId(tenantId);
		event.setWmsTenantId(wmsTenantId);
		event.setErpTenantId(erpTenantId);
		event.setWarehouseId(warehouseId);
		event.setEventType(context.getEventType().name());
		event.setSourceType(context.getSourceType());
		event.setSourceId(context.getSourceId());
		event.setSourceNo(context.getSourceNo());
		event.setOperatorId(context.getOperatorId());
		event.setOperatorName(context.getOperatorName());
		event.setReason(context.getReason());
		event.setIdempotencyKey(context.getIdempotencyKey());
		event.setOccurredAt(now);
		event.setCreateTime(now);
		Assert.isTrue(eventMapper.insert(event) == 1, "库存事件保存失败");

		int lineNo = 1;
		for (InventoryMutationLine source : lines) {
			validateLine(source);
			InventoryEventLine line = new InventoryEventLine();
			line.setEventId(event.getId());
			line.setLineNo(lineNo++);
			line.setInventoryId(source.getInventoryId());
			line.setLocationId(source.getLocationId());
			line.setCounterpartLocationId(source.getCounterpartLocationId());
			line.setSkuCode(source.getSkuCode());
			line.setQuality(LocationInventoryQuality.normalize(source.getQuality()));
			line.setQuantityDelta(source.getQuantityDelta());
			line.setReservedDelta(source.getReservedDelta());
			line.setBeforeQuantity(source.getBeforeQuantity());
			line.setAfterQuantity(source.getAfterQuantity());
			line.setBeforeReserved(source.getBeforeReserved());
			line.setAfterReserved(source.getAfterReserved());
			line.setCreateTime(now);
			Assert.isTrue(lineMapper.insert(line) == 1, "库存事件明细保存失败");
		}
		return event;
	}

	private void validateLine(InventoryMutationLine line) {
		Assert.notNull(line, "库存事件明细不能为空");
		Assert.notNull(line.getLocationId(), "库存事件库位不能为空");
		Assert.hasText(line.getSkuCode(), "库存事件SKU不能为空");
		Assert.notNull(line.getQuantityDelta(), "库存实物变化不能为空");
		Assert.notNull(line.getReservedDelta(), "库存预占变化不能为空");
		Assert.notNull(line.getBeforeQuantity(), "变更前实物库存不能为空");
		Assert.notNull(line.getAfterQuantity(), "变更后实物库存不能为空");
		Assert.notNull(line.getBeforeReserved(), "变更前预占库存不能为空");
		Assert.notNull(line.getAfterReserved(), "变更后预占库存不能为空");
		Assert.isTrue(line.getAfterQuantity() >= 0, "变更后实物库存不能为负数");
		Assert.isTrue(line.getAfterReserved() >= 0 && line.getAfterReserved() <= line.getAfterQuantity(),
				"变更后预占库存必须介于0和实物库存之间");
	}
}
