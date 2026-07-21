package com.erp.admin.wms.service;

import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.wms.converter.TransferOrderItemConverter;
import com.erp.admin.wms.mapper.TransferOrderItemMapper;
import com.erp.admin.wms.model.dto.TransferOrderItemDTO;
import com.erp.admin.wms.model.entity.TransferOrderItem;
import com.erp.admin.wms.model.vo.TransferOrderItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 调拨单明细服务
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferOrderItemService extends ExtendServiceImpl<TransferOrderItemMapper, TransferOrderItem> {

	private final SkuBriefService skuBriefService;

	/**
	 * 根据调拨单ID查询明细列表
	 * @param transferOrderId 调拨单ID
	 * @return 明细列表
	 */
	public List<TransferOrderItem> getByTransferOrderId(Long transferOrderId) {
		return baseMapper.selectByTransferOrderId(transferOrderId);
	}

	/**
	 * 根据调拨单ID查询明细VO列表（含SKU简要信息）
	 * @param transferOrderId 调拨单ID
	 * @return 明细VO列表
	 */
	public List<TransferOrderItemVO> getVoListByTransferOrderId(Long transferOrderId) {
		List<TransferOrderItem> items = baseMapper.selectByTransferOrderId(transferOrderId);
		if (items == null || items.isEmpty()) {
			return Collections.emptyList();
		}
		List<TransferOrderItemVO> voList = TransferOrderItemConverter.INSTANCE.entityListToVoList(items);
		skuBriefService.enrichForQuery(voList, TransferOrderItemVO::getSkuCode, TransferOrderItemVO::setSkuBrief);
		return voList;
	}

	/**
	 * 批量保存明细
	 * @param transferOrderId 调拨单ID
	 * @param itemDTOs 明细DTO列表
	 * @return 保存后的明细列表
	 */
	@Transactional(rollbackFor = Exception.class)
	public List<TransferOrderItem> batchSave(Long transferOrderId, Long erpTenantId, List<TransferOrderItemDTO> itemDTOs) {
		List<TransferOrderItem> items = TransferOrderItemConverter.INSTANCE.dtoListToEntityList(itemDTOs);

		for (TransferOrderItem item : items) {
			item.setTransferOrderId(transferOrderId);
			// 货主取自单头（整单归属一个货主）
			item.setErpTenantId(erpTenantId);
		}

		this.saveBatch(items);
		log.info("Batch saved {} items for transferOrderId={}", items.size(), transferOrderId);
		return items;
	}

	/**
	 * 根据调拨单ID删除明细
	 * @param transferOrderId 调拨单ID
	 * @return 删除数量
	 */
	@Transactional(rollbackFor = Exception.class)
	public int deleteByTransferOrderId(Long transferOrderId) {
		int count = baseMapper.deleteByTransferOrderId(transferOrderId);
		log.info("Deleted {} items for transferOrderId={}", count, transferOrderId);
		return count;
	}

}
