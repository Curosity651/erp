package com.erp.admin.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentPickTaskLineMapper;
import com.erp.admin.wms.model.dto.FulfillmentCancelReturnScanDTO;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskLine;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class FulfillmentStatusSyncService {
	private final WmsFulfillmentOrderMapper orderMapper;
	private final WmsFulfillmentPickTaskLineMapper lineMapper;
	private final LocationInventoryService inventoryService;
	private final ErpOrderMapper erpOrderMapper;

	@Transactional(rollbackFor = Exception.class)
	public void platformCancelled(String sourceType, Long sourceOrderId, String reason) {
		WmsFulfillmentOrder order = orderMapper.selectBySource(sourceType, sourceOrderId);
		if (order == null || order.getFulfillmentStatus() == FulfillmentStatus.CANCELLED
				|| order.getFulfillmentStatus() == FulfillmentStatus.SHIPPED) return;
		FulfillmentStatus status = order.getFulfillmentStatus();
		if (status == FulfillmentStatus.WAITING_SHELF || status == FulfillmentStatus.WAITING_PICK) {
			inventoryService.release(order.getId());
			Assert.isTrue(orderMapper.transit(order.getId(), status, FulfillmentStatus.CANCELLED) == 1,
					"取消状态已变化");
			updateErp(order, FulfillmentStatus.CANCELLED);
			return;
		}
		if (status == FulfillmentStatus.PICKING || status == FulfillmentStatus.WAITING_PACK
				|| status == FulfillmentStatus.PACKED) {
			Assert.isTrue(orderMapper.transit(order.getId(), status,
					FulfillmentStatus.CANCEL_RETURNING) == 1, "取消状态已变化");
			order.setCancelReason(reason);
			orderMapper.updateById(order);
			updateErp(order, FulfillmentStatus.CANCEL_RETURNING);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void scanReturn(FulfillmentCancelReturnScanDTO dto) {
		WmsFulfillmentOrder order = orderMapper.selectForUpdate(dto.getFulfillmentOrderId());
		Assert.notNull(order, "履约订单不存在");
		Assert.isTrue(order.getFulfillmentStatus() == FulfillmentStatus.CANCEL_RETURNING,
				"订单不是取消回退状态");
		WmsFulfillmentPickTaskLine line = lineMapper.selectOne(
				Wrappers.<WmsFulfillmentPickTaskLine>lambdaQuery()
						.eq(WmsFulfillmentPickTaskLine::getFulfillmentOrderId, order.getId())
						.eq(WmsFulfillmentPickTaskLine::getLocationCode, dto.getLocationCode())
						.eq(WmsFulfillmentPickTaskLine::getWarehouseSkuCode, dto.getWarehouseSkuCode())
						.apply("returned_quantity < picked_quantity").last("LIMIT 1"));
		Assert.notNull(line, "退回库位或内部 SKU 与原拣货记录不匹配");
		Assert.isTrue(lineMapper.addReturned(line.getId(), dto.getQuantity(), line.getVersion()) == 1,
				"退回数量超出已拣数量或数据已变化");
		long remaining = lineMapper.selectCount(Wrappers.<WmsFulfillmentPickTaskLine>lambdaQuery()
				.eq(WmsFulfillmentPickTaskLine::getFulfillmentOrderId, order.getId())
				.apply("returned_quantity < picked_quantity"));
		if (remaining == 0) {
			inventoryService.release(order.getId());
			Assert.isTrue(orderMapper.transit(order.getId(), FulfillmentStatus.CANCEL_RETURNING,
					FulfillmentStatus.CANCELLED) == 1, "取消完成状态更新失败");
			updateErp(order, FulfillmentStatus.CANCELLED);
		}
	}

	private void updateErp(WmsFulfillmentOrder fulfillment, FulfillmentStatus status) {
		if ("MANUAL".equals(fulfillment.getSourceType()) || fulfillment.getSourceOrderId() == null) return;
		ErpOrder order = erpOrderMapper.selectById(fulfillment.getSourceOrderId());
		if (order != null) {
			order.setWarehouseFulfillmentStatus(status.name());
			erpOrderMapper.updateById(order);
		}
	}
}
