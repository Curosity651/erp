package com.erp.admin.wms.service.platform;

import java.util.Collections;

import com.erp.admin.order.model.vo.LabelBatchFileVO;
import com.erp.admin.order.model.vo.LabelBatchVO;
import com.erp.admin.order.service.common.model.ConfirmResult;
import com.erp.admin.order.service.label.LabelPrintOrchestrator;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

abstract class PlatformAdapterSupport {
	protected PlatformActionResult result(ConfirmResult result, Long erpOrderId) {
		if (result != null && result.getItems() != null) {
			for (ConfirmResult.Item item : result.getItems()) {
				if (erpOrderId.equals(item.getOrderId())) {
					return item.isSuccess()
							? PlatformActionResult.success(item.getMessage(), item.getSupplyId())
							: PlatformActionResult.failure(item.getMessage());
				}
			}
		}
		return PlatformActionResult.failure("平台未返回该订单的处理结果");
	}

	protected PlatformLabelResult printLabel(String platform, WmsFulfillmentOrder order,
			LabelPrintOrchestrator orchestrator) {
		LabelBatchVO batch = orchestrator.printLabels(platform,
				Collections.singletonList(order.getSourceOrderId()), 0L,
				"海外仓履约单 " + order.getFulfillmentNo());
		if (batch == null || (batch.getFailedCount() != null && batch.getFailedCount() > 0)
				|| CollectionUtils.isEmpty(batch.getFiles())) {
			return new PlatformLabelResult(false, "平台面单生成失败", null, null, null);
		}
		String url = null;
		for (LabelBatchFileVO file : batch.getFiles()) {
			if (StringUtils.hasText(file.getDownloadUrl())) {
				url = file.getDownloadUrl();
				break;
			}
		}
		if (!StringUtils.hasText(url)) {
			return new PlatformLabelResult(false, "面单文件尚未生成", null, null, null);
		}
		return PlatformLabelResult.success(String.valueOf(batch.getBatchId()), url, order.getSourceOrderNo());
	}
}

