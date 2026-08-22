package com.erp.admin.order.mapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.order.model.entity.ErpLabelBatchItem;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.Collection;
import java.util.List;

public interface ErpLabelBatchItemMapper extends ExtendMapper<ErpLabelBatchItem> {
	default List<ErpLabelBatchItem> selectByBatchId(Long batchId) {
		LambdaQueryWrapperX<ErpLabelBatchItem> w = WrappersX.lambdaQueryX(ErpLabelBatchItem.class)
				.eq(ErpLabelBatchItem::getBatchId, batchId);
		return this.selectList(w);
	}

	default Long selectLatestBatchIdByOrderIds(Collection<Long> orderIds) {
		if (orderIds == null || orderIds.isEmpty()) {
			return null;
		}
		List<ErpLabelBatchItem> items = this.selectList(
				WrappersX.lambdaQueryX(ErpLabelBatchItem.class)
						.select(ErpLabelBatchItem::getBatchId)
						.in(ErpLabelBatchItem::getOrderId, orderIds)
						.orderByDesc(ErpLabelBatchItem::getBatchId)
						.last("limit 1"));
		return items.isEmpty() ? null : items.get(0).getBatchId();
	}

	default int updateItemStatusAndFail(Long batchId, Long orderId, String status, String failCode, String errorMsg) {
		LambdaUpdateWrapper<ErpLabelBatchItem> uw = Wrappers.lambdaUpdate(ErpLabelBatchItem.class);
		uw.eq(ErpLabelBatchItem::getBatchId, batchId).eq(ErpLabelBatchItem::getOrderId, orderId)
				.set(ErpLabelBatchItem::getStatus, status)
				.set(ErpLabelBatchItem::getFailCode, failCode)
				.set(ErpLabelBatchItem::getErrorMsg, errorMsg);
		return this.update(null, uw);
	}

	default int batchUpdateOrderFileId(Long batchId, Collection<Long> orderIds, Long fileId) {
		if (orderIds == null || orderIds.isEmpty()) return 0;
		LambdaUpdateWrapper<ErpLabelBatchItem> uw =
				Wrappers.lambdaUpdate(ErpLabelBatchItem.class);
		uw.eq(ErpLabelBatchItem::getBatchId, batchId).in(ErpLabelBatchItem::getOrderId, orderIds)
				.set(ErpLabelBatchItem::getOrderFileId, fileId);
		return this.update(null, uw);
	}

	default int batchUpdateSupplyFileId(Long batchId, Collection<Long> orderIds, Long fileId) {
		if (orderIds == null || orderIds.isEmpty()) return 0;
		LambdaUpdateWrapper<ErpLabelBatchItem> uw =
				Wrappers.lambdaUpdate(ErpLabelBatchItem.class);
		uw.eq(ErpLabelBatchItem::getBatchId, batchId).in(ErpLabelBatchItem::getOrderId, orderIds)
				.set(ErpLabelBatchItem::getSupplyFileId, fileId);
		return this.update(null, uw);
	}

	default int batchUpdateStatus(Long batchId, Collection<Long> orderIds, String status) {
		if (orderIds == null || orderIds.isEmpty()) return 0;
		LambdaUpdateWrapper<ErpLabelBatchItem> uw =
				Wrappers.lambdaUpdate(ErpLabelBatchItem.class);
		uw.eq(ErpLabelBatchItem::getBatchId, batchId).in(ErpLabelBatchItem::getOrderId, orderIds)
				.set(ErpLabelBatchItem::getStatus, status);
		return this.update(null, uw);
	}
}

