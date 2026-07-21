package com.erp.admin.wms.service.handler;

import com.erp.admin.wms.mapper.PurchaseOrderFileMapper;
import com.erp.admin.wms.model.entity.PurchaseOrder;
import com.erp.admin.wms.model.entity.PurchaseOrderFile;
import com.erp.admin.wms.model.enums.PurchaseOrderFileType;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 其他附件处理器
 * <p>
 * 处理其他类型附件的增删
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtherFileHandler {

	private final PurchaseOrderFileMapper purchaseOrderFileMapper;

	private final FileTypeValidator fileTypeValidator;

	/**
	 * 处理其他附件变更
	 *
	 * @param order        采购单实体
	 * @param otherFileIds 其他附件文件ID列表
	 */
	@Transactional(rollbackFor = Exception.class)
	public void handleOtherFilesChange(PurchaseOrder order, List<Long> otherFileIds) {
		if (otherFileIds == null) {
			return;
		}

		// 校验采购单状态
		validateOrderStatus(order);

		// 获取现有的其他附件
		List<PurchaseOrderFile> existingFiles = getExistingOtherFiles(order.getId());
		Set<Long> existingSysFileIds = existingFiles.stream()
				.map(PurchaseOrderFile::getSysFileId)
				.collect(Collectors.toSet());

		// 提交的文件ID集合
		Set<Long> submittedFileIds = new HashSet<>(otherFileIds);

		// 新增的文件
		for (Long sysFileId : otherFileIds) {
			if (!existingSysFileIds.contains(sysFileId)) {
				// 校验文件存在
				fileTypeValidator.validateFileExists(sysFileId);

				// 新增附件关联
				saveOtherFile(order.getId(), sysFileId);
				log.info("Other file added, orderId={}, sysFileId={}", order.getId(), sysFileId);
			}
		}

		// 删除不在提交列表中的附件
		for (PurchaseOrderFile existing : existingFiles) {
			if (!submittedFileIds.contains(existing.getSysFileId())) {
				purchaseOrderFileMapper.deleteById(existing.getId());
				log.info("Other file removed, orderId={}, sysFileId={}",
						order.getId(), existing.getSysFileId());
			}
		}
	}

	/**
	 * 获取现有的其他附件
	 */
	private List<PurchaseOrderFile> getExistingOtherFiles(Long orderId) {
		LambdaQueryWrapperX<PurchaseOrderFile> wrapper = WrappersX.lambdaQueryX(PurchaseOrderFile.class)
				.eq(PurchaseOrderFile::getPurchaseOrderId, orderId)
				.eq(PurchaseOrderFile::getFileType, PurchaseOrderFileType.OTHER.name());
		return purchaseOrderFileMapper.selectList(wrapper);
	}

	/**
	 * 保存其他附件关联
	 */
	private void saveOtherFile(Long orderId, Long sysFileId) {
		PurchaseOrderFile file = new PurchaseOrderFile();
		file.setPurchaseOrderId(orderId);
		file.setSysFileId(sysFileId);
		file.setFileType(PurchaseOrderFileType.OTHER.name());
		file.setCreateTime(LocalDateTime.now());
		purchaseOrderFileMapper.insert(file);
	}

	/**
	 * 校验采购单状态是否允许修改其他附件
	 */
	private void validateOrderStatus(PurchaseOrder order) {
		PurchaseOrderStatus status = PurchaseOrderStatus.valueOf(order.getOrderStatus());
		Assert.isTrue(status != PurchaseOrderStatus.COMPLETED && status != PurchaseOrderStatus.CANCELLED,
				"已完成或已取消的采购单不允许修改附件");
	}

}
