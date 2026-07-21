package com.erp.admin.order.service.label;

import com.erp.admin.order.model.entity.ErpLabelBatchFile;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.service.LabelConstants;
import com.erp.admin.order.service.LabelService;
import com.erp.admin.order.util.Base64ImagesToPdf;
import com.erp.admin.order.util.PdfMergeUtil;
import com.erp.admin.product.model.entity.Sku;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 面单 PDF 生成组件
 * <p>
 * 职责：
 * - 提供图片转PDF和PDF合并两种业务方法
 * - 封装 收集数据 → 生成PDF → 上传 → 保存记录 → 更新状态 的完整流程
 * - 统一异常处理
 * <p>
 * 使用场景：WB 和 Ozon 面单打印服务的 PDF 生成
 *
 * @author system
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LabelPdfGenerator {

	private final LabelService labelService;
	private final LabelFileNameBuilder fileNameBuilder;

	/**
	 * 生成订单面单 PDF（多图片 → 单PDF）
	 * <p>
	 * 流程: 收集图片 → 图片转PDF → 上传 → 保存记录 → 更新状态
	 *
	 * @param context        PDF 生成上下文
	 * @param imageCollector 图片收集函数
	 */
	public void generateImagesPdf(
			PdfGenerationContext context,
			Function<List<ErpOrder>, List<String>> imageCollector) {

		List<ErpOrder> orders = context.getGroup().getOrders();

		// 1. 收集图片
		List<String> images = imageCollector.apply(orders);

		if (images.isEmpty()) {
			handleEmptyData(context, orders);
			return;
		}

		// 2. 生成文件名
		String fileName = buildFileName(context, orders.size());

		try {
			// 3. 图片转PDF字节
			byte[] pdfBytes = Base64ImagesToPdf.writeImagesToPdf(images);

			// 4. 上传到OSS
			String objectKey = labelService.uploadPdfToOss(pdfBytes, context.getBatchNo(), fileName);

			// 5. 保存记录并更新状态
			saveAndUpdateStatus(context, fileName, objectKey, images.size(), orders);

		} catch (Exception e) {
			handleFailure(context, fileName, orders, e);
		}
	}

	/**
	 * 合并多个 PDF（多PDF → 单PDF）
	 * <p>
	 * 流程: 收集PDF → 合并PDF → 上传 → 保存记录 → 更新状态
	 *
	 * @param context      PDF 生成上下文
	 * @param pdfCollector PDF 收集函数
	 */
	public void mergePdfs(
			PdfGenerationContext context,
			Function<List<ErpOrder>, List<String>> pdfCollector) {

		List<ErpOrder> orders = context.getGroup().getOrders();

		// 1. 收集PDF
		List<String> pdfs = pdfCollector.apply(orders);

		if (pdfs.isEmpty()) {
			handleEmptyData(context, orders);
			return;
		}

		// 2. 生成文件名
		String fileName = buildFileName(context, orders.size());

		try {
			// 3. 合并PDF字节
			byte[] pdfBytes = PdfMergeUtil.mergePdfs(pdfs);

			// 4. 上传到OSS
			String objectKey = labelService.uploadPdfToOss(pdfBytes, context.getBatchNo(), fileName);

			// 5. 保存记录并更新状态
			saveAndUpdateStatus(context, fileName, objectKey, pdfs.size(), orders);

		} catch (Exception e) {
			handleFailure(context, fileName, orders, e);
		}
	}

	// ========== 私有辅助方法 ==========

	/**
	 * 构建文件名
	 */
	private String buildFileName(PdfGenerationContext context, int orderCount) {
		return fileNameBuilder.buildFileName(
				context.getBatchNo(),
				context.getFileType(),
				context.getGroup().getDestinationWarehouseName(),
				context.getGroup().getSku(),
				orderCount
		);
	}

	/**
	 * 保存文件记录并更新订单状态
	 */
	private void saveAndUpdateStatus(
			PdfGenerationContext context,
			String fileName,
			String objectKey,
			int pageCount,
			List<ErpOrder> orders) {

		// 保存文件记录
		ErpLabelBatchFile fileRecord = buildFileRecord(context, fileName, objectKey, pageCount);
		Long fileId = labelService.saveFileRecord(fileRecord);

		// 更新订单项状态
		List<Long> orderIds = orders.stream()
				.map(ErpOrder::getId)
				.collect(Collectors.toList());
		labelService.batchUpdateItemFileId(context.getBatchId(), orderIds, fileId, "ORDER");
		labelService.batchUpdateItemStatus(context.getBatchId(), orderIds, LabelConstants.ITEM_STATUS_SUCCESS);
	}

	/**
	 * 处理空数据情况
	 */
	private void handleEmptyData(PdfGenerationContext context, List<ErpOrder> orders) {
		log.warn("[{}][LABEL] 分组无有效面单: groupKey={}", context.getPlatform(), context.getGroupKey());
		markOrdersFailure(context.getBatchId(), orders,
				LabelConstants.ITEM_FAIL_ORDER_LABEL_MISSING, "订单面单缺失");
	}

	/**
	 * 处理PDF生成失败
	 */
	private void handleFailure(
			PdfGenerationContext context,
			String fileName,
			List<ErpOrder> orders,
			Exception e) {

		log.error("[{}][LABEL] PDF生成失败: batchNo={}, fileName={}, error={}",
				context.getPlatform(), context.getBatchNo(), fileName, e.getMessage(), e);

		markOrdersFailure(context.getBatchId(), orders,
				LabelConstants.ITEM_FAIL_FILE_UPLOAD, "PDF生成失败");
	}

	/**
	 * 批量标记订单失败
	 *
	 * @param batchId     批次ID
	 * @param orders      订单列表
	 * @param failureCode 失败码
	 * @param failureMsg  失败消息
	 */
	private void markOrdersFailure(Long batchId, List<ErpOrder> orders, String failureCode, String failureMsg) {
		for (ErpOrder order : orders) {
			labelService.updateItemFailure(batchId, order.getId(), failureCode, failureMsg);
		}
	}

	/**
	 * 构建文件记录
	 */
	private ErpLabelBatchFile buildFileRecord(
			PdfGenerationContext context,
			String fileName,
			String objectKey,
			int pageCount) {

		OrderGrouper.OrderGroup group = context.getGroup();
		Sku sku = group.getSku();

		ErpLabelBatchFile fileRecord = new ErpLabelBatchFile();
		fileRecord.setBatchId(context.getBatchId());
		fileRecord.setType(context.getFileRecordType());
		fileRecord.setDestinationWarehouseId(group.getDestinationWarehouseId());
		fileRecord.setDestinationWarehouseName(group.getDestinationWarehouseName());
		fileRecord.setErpSkuCode(sku == null ? "{skuCode}" : sku.getSkuCode());
		fileRecord.setErpSkuNo(sku == null ? "{skuNo}" : String.valueOf(sku.getSkuNo()));
		fileRecord.setSkuCount(group.getOrders().size());
		fileRecord.setFileName(fileName);
		fileRecord.setObjectKey(objectKey);
		fileRecord.setPageCount(pageCount);

		return fileRecord;
	}

	/**
	 * PDF 生成上下文
	 * <p>
	 * 封装 PDF 生成所需的所有参数
	 */
	@Getter
	public static class PdfGenerationContext {
		private final Long batchId;
		private final String batchNo;
		private final String platform;
		private final String groupKey;
		private final OrderGrouper.OrderGroup group;
		private final String fileType;          // 文件名中的类型（如 "ORDER", "SUPPLY", "OZON"）
		private final String fileRecordType;    // 文件记录类型（如 "WB_ORDER_PDF", "OZON_PDF"）

		public PdfGenerationContext(
				Long batchId,
				String batchNo,
				String platform,
				String groupKey,
				OrderGrouper.OrderGroup group,
				String fileType,
				String fileRecordType) {
			this.batchId = batchId;
			this.batchNo = batchNo;
			this.platform = platform;
			this.groupKey = groupKey;
			this.group = group;
			this.fileType = fileType;
			this.fileRecordType = fileRecordType;
		}

	}
}
