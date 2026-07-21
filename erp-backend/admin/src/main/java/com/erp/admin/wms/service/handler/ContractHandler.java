package com.erp.admin.wms.service.handler;

import com.erp.admin.wms.mapper.PurchaseOrderFileMapper;
import com.erp.admin.wms.model.dto.ContractInfoDTO;
import com.erp.admin.wms.model.entity.PurchaseOrder;
import com.erp.admin.wms.model.entity.PurchaseOrderFile;
import com.erp.admin.wms.model.enums.PurchaseOrderFileType;
import com.erp.admin.wms.model.enums.PurchaseOrderStatus;
import com.erp.admin.wms.service.validator.FileTypeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

/**
 * 合同处理器
 * <p>
 * 处理合同文件的上传和替换（合同文件不支持删除）
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContractHandler {

	private final PurchaseOrderFileMapper purchaseOrderFileMapper;

	private final FileTypeValidator fileTypeValidator;

	/**
	 * 处理合同信息变更
	 *
	 * @param order        采购单实体
	 * @param contractInfo 合同信息DTO
	 */
	@Transactional(rollbackFor = Exception.class)
	public void handleContractChange(PurchaseOrder order, ContractInfoDTO contractInfo) {
		if (contractInfo == null || contractInfo.getContractFileId() == null) {
			return;
		}

		// 校验采购单状态
		validateOrderStatus(order);

		String action = contractInfo.getAction();
		if (action == null) {
			// 无操作，跳过
			return;
		}

		// 校验文件类型
		fileTypeValidator.validateContractFile(contractInfo.getContractFileId());

		if (ContractInfoDTO.ACTION_UPLOAD.equals(action)) {
			// 上传新合同
			handleUpload(order.getId(), contractInfo.getContractFileId());
		} else if (ContractInfoDTO.ACTION_REPLACE.equals(action)) {
			// 替换合同
			handleReplace(order.getId(), contractInfo.getContractFileId());
		}
	}

	/**
	 * 处理合同上传
	 */
	private void handleUpload(Long orderId, Long sysFileId) {
		// 检查是否已有合同
		PurchaseOrderFile existingContract = purchaseOrderFileMapper.selectByOrderIdAndType(
				orderId, PurchaseOrderFileType.CONTRACT.name());

		if (existingContract != null) {
			// 已有合同，执行替换逻辑
			handleReplace(orderId, sysFileId);
			return;
		}

		// 保存新合同
		saveContractFile(orderId, sysFileId);
		log.info("Contract uploaded, orderId={}, sysFileId={}", orderId, sysFileId);
	}

	/**
	 * 处理合同替换
	 */
	private void handleReplace(Long orderId, Long sysFileId) {
		// 查找旧合同
		PurchaseOrderFile existingContract = purchaseOrderFileMapper.selectByOrderIdAndType(
				orderId, PurchaseOrderFileType.CONTRACT.name());

		if (existingContract != null) {
			// 检查是否是同一个文件
			if (existingContract.getSysFileId().equals(sysFileId)) {
				// 同一个文件，无需替换
				return;
			}

			// 删除旧合同关联（注意：不删除sys_file中的文件，只删除关联关系）
			purchaseOrderFileMapper.deleteById(existingContract.getId());
			log.info("Old contract removed, orderId={}, oldSysFileId={}",
					orderId, existingContract.getSysFileId());
		}

		// 保存新合同
		saveContractFile(orderId, sysFileId);
		log.info("Contract replaced, orderId={}, newSysFileId={}", orderId, sysFileId);
	}

	/**
	 * 保存合同文件关联
	 */
	private void saveContractFile(Long orderId, Long sysFileId) {
		PurchaseOrderFile file = new PurchaseOrderFile();
		file.setPurchaseOrderId(orderId);
		file.setSysFileId(sysFileId);
		file.setFileType(PurchaseOrderFileType.CONTRACT.name());
		file.setCreateTime(LocalDateTime.now());
		purchaseOrderFileMapper.insert(file);
	}

	/**
	 * 校验采购单状态是否允许修改合同
	 */
	private void validateOrderStatus(PurchaseOrder order) {
		PurchaseOrderStatus status = PurchaseOrderStatus.valueOf(order.getOrderStatus());
		Assert.isTrue(status != PurchaseOrderStatus.COMPLETED && status != PurchaseOrderStatus.CANCELLED,
				"已完成或已取消的采购单不允许修改合同");
	}

}
