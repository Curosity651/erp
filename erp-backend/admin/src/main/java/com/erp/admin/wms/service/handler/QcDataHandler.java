package com.erp.admin.wms.service.handler;

import com.erp.admin.wms.mapper.PurchaseOrderQcItemMapper;
import com.erp.admin.wms.model.dto.QcDataDTO;
import com.erp.admin.wms.model.dto.QcItemDTO;
import com.erp.admin.wms.model.entity.PurchaseOrder;
import com.erp.admin.wms.model.entity.PurchaseOrderQcItem;
import com.erp.admin.wms.model.enums.PurchaseOrderStatus;
import com.erp.admin.wms.service.validator.FileTypeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.toolkit.WrappersX;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 质检数据处理器
 * <p>
 * 处理质检数据的增删改
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QcDataHandler {

	private final PurchaseOrderQcItemMapper purchaseOrderQcItemMapper;

	private final FileTypeValidator fileTypeValidator;

	/**
	 * 处理质检数据变更
	 *
	 * @param order  采购单实体
	 * @param qcData 质检数据DTO
	 */
	@Transactional(rollbackFor = Exception.class)
	public void handleQcDataChange(PurchaseOrder order, QcDataDTO qcData) {
		if (qcData == null) {
			return;
		}

		// 校验采购单状态
		validateOrderStatus(order);

		// 处理各SKU的质检数据
		if (qcData.getItems() != null && !qcData.getItems().isEmpty()) {
			handleQcItems(order.getId(), qcData.getItems());
		}
	}

	/**
	 * 处理质检数据项列表
	 */
	private void handleQcItems(Long orderId, List<QcItemDTO> items) {
		// 过滤掉空数据行（只有skuCode，没有实际质检数据的行）
		List<QcItemDTO> validItems = items.stream()
				.filter(item -> !isEmptyQcItem(item))
				.collect(Collectors.toList());

		// 获取现有的质检数据
		List<PurchaseOrderQcItem> existingItems = getExistingQcItems(orderId);
		Map<String, PurchaseOrderQcItem> existingMap = existingItems.stream()
				.collect(Collectors.toMap(PurchaseOrderQcItem::getSkuCode, item -> item));

		// 收集有效提交的SKU编码
		Set<String> submittedSkuCodes = validItems.stream()
				.map(QcItemDTO::getSkuCode)
				.collect(Collectors.toSet());

		// 处理每个提交的质检数据
		for (QcItemDTO item : validItems) {
			// 校验毛重 >= 净重
			validateWeight(item);

			// 校验质检报告文件类型
			if (item.getQcFileId() != null) {
				fileTypeValidator.validateQcReportFile(item.getQcFileId());
			}

			PurchaseOrderQcItem existing = existingMap.get(item.getSkuCode());
			if (existing != null) {
				// 更新现有记录
				updateQcItem(existing, item);
			} else {
				// 新增记录
				createQcItem(orderId, item);
			}
		}

		// 删除不在提交列表中的质检数据
		for (PurchaseOrderQcItem existing : existingItems) {
			if (!submittedSkuCodes.contains(existing.getSkuCode())) {
				purchaseOrderQcItemMapper.deleteById(existing.getId());
				log.info("QC item deleted, orderId={}, skuCode={}", orderId, existing.getSkuCode());
			}
		}
	}

	/**
	 * 获取现有的质检数据
	 */
	private List<PurchaseOrderQcItem> getExistingQcItems(Long orderId) {
		LambdaQueryWrapperX<PurchaseOrderQcItem> wrapper = WrappersX.lambdaQueryX(PurchaseOrderQcItem.class)
				.eq(PurchaseOrderQcItem::getPurchaseOrderId, orderId);
		return purchaseOrderQcItemMapper.selectList(wrapper);
	}

	/**
	 * 创建质检数据
	 */
	private void createQcItem(Long orderId, QcItemDTO item) {
		PurchaseOrderQcItem entity = new PurchaseOrderQcItem();
		entity.setPurchaseOrderId(orderId);
		entity.setSkuCode(item.getSkuCode());
		entity.setLengthCm(item.getLengthCm());
		entity.setWidthCm(item.getWidthCm());
		entity.setHeightCm(item.getHeightCm());
		entity.setGrossWeightKg(item.getGrossWeightKg());
		entity.setNetWeightKg(item.getNetWeightKg());
		entity.setQcFileId(item.getQcFileId());
		entity.setCreateTime(LocalDateTime.now());

		purchaseOrderQcItemMapper.insert(entity);
		log.info("QC item created, orderId={}, skuCode={}", orderId, item.getSkuCode());
	}

	/**
	 * 更新质检数据
	 */
	private void updateQcItem(PurchaseOrderQcItem existing, QcItemDTO item) {
		boolean changed = false;

		if (item.getLengthCm() != null && !item.getLengthCm().equals(existing.getLengthCm())) {
			existing.setLengthCm(item.getLengthCm());
			changed = true;
		}
		if (item.getWidthCm() != null && !item.getWidthCm().equals(existing.getWidthCm())) {
			existing.setWidthCm(item.getWidthCm());
			changed = true;
		}
		if (item.getHeightCm() != null && !item.getHeightCm().equals(existing.getHeightCm())) {
			existing.setHeightCm(item.getHeightCm());
			changed = true;
		}
		if (item.getGrossWeightKg() != null && !item.getGrossWeightKg().equals(existing.getGrossWeightKg())) {
			existing.setGrossWeightKg(item.getGrossWeightKg());
			changed = true;
		}
		if (item.getNetWeightKg() != null && !item.getNetWeightKg().equals(existing.getNetWeightKg())) {
			existing.setNetWeightKg(item.getNetWeightKg());
			changed = true;
		}
		// 质检报告文件可以为null（清空）
		if ((item.getQcFileId() == null && existing.getQcFileId() != null) ||
				(item.getQcFileId() != null && !item.getQcFileId().equals(existing.getQcFileId()))) {
			existing.setQcFileId(item.getQcFileId());
			changed = true;
		}

		if (changed) {
			existing.setUpdateTime(LocalDateTime.now());
			purchaseOrderQcItemMapper.updateById(existing);
			log.info("QC item updated, orderId={}, skuCode={}",
					existing.getPurchaseOrderId(), existing.getSkuCode());
		}
	}

	/**
	 * 校验毛重 >= 净重
	 */
	private void validateWeight(QcItemDTO item) {
		if (item.getGrossWeightKg() != null && item.getNetWeightKg() != null) {
			Assert.isTrue(item.getGrossWeightKg().compareTo(item.getNetWeightKg()) >= 0,
					String.format("SKU[%s]的毛重必须大于等于净重", item.getSkuCode()));
		}
	}

	/**
	 * 校验采购单状态是否允许修改质检数据
	 */
	private void validateOrderStatus(PurchaseOrder order) {
		PurchaseOrderStatus status = PurchaseOrderStatus.valueOf(order.getOrderStatus());
		Assert.isTrue(status != PurchaseOrderStatus.COMPLETED && status != PurchaseOrderStatus.CANCELLED,
				"已完成或已取消的采购单不允许修改质检数据");
	}

	/**
	 * 判断质检数据是否为空行（只有skuCode，没有实际质检数据）
	 * <p>
	 * 前端会根据采购明细自动生成空的质检数据行，提交时需要过滤掉这些空行
	 *
	 * @param item 质检数据项
	 * @return 是否为空行
	 */
	private boolean isEmptyQcItem(QcItemDTO item) {
		return item.getLengthCm() == null
				&& item.getWidthCm() == null
				&& item.getHeightCm() == null
				&& item.getGrossWeightKg() == null
				&& item.getNetWeightKg() == null
				&& item.getQcFileId() == null;
	}

}
