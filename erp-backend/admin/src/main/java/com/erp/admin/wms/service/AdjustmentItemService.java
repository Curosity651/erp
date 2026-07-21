package com.erp.admin.wms.service;

import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.wms.converter.AdjustmentItemConverter;
import com.erp.admin.wms.mapper.AdjustmentItemMapper;
import com.erp.admin.wms.model.dto.AdjustmentItemDTO;
import com.erp.admin.wms.model.entity.AdjustmentOrderItem;
import com.erp.admin.wms.model.vo.AdjustmentItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 调整单明细服务
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdjustmentItemService extends ExtendServiceImpl<AdjustmentItemMapper, AdjustmentOrderItem> {

	private final SkuBriefService skuBriefService;

	/**
	 * 根据调整单ID查询明细列表
	 * @param adjustmentOrderId 调整单ID
	 * @return 明细列表
	 */
	public List<AdjustmentOrderItem> getByAdjustmentOrderId(Long adjustmentOrderId) {
		return baseMapper.selectByAdjustmentOrderId(adjustmentOrderId);
	}

	/**
	 * 根据调整单ID查询明细VO列表（含SKU展示信息）
	 * @param adjustmentOrderId 调整单ID
	 * @return 明细VO列表
	 */
	public List<AdjustmentItemVO> getVoListByAdjustmentOrderId(Long adjustmentOrderId) {
		List<AdjustmentOrderItem> adjustmentOrderItems = this.baseMapper.selectByAdjustmentOrderId(adjustmentOrderId);
		if (adjustmentOrderItems == null || adjustmentOrderItems.isEmpty()) {
			return new ArrayList<>();
		}
		List<AdjustmentItemVO> voList = AdjustmentItemConverter.INSTANCE.entityListToVOList(adjustmentOrderItems);
		// 填充 SKU 展示信息
		skuBriefService.enrichForQuery(
				voList,
				AdjustmentItemVO::getSkuCode,
				AdjustmentItemVO::setSkuBrief
		);
		return voList;
	}

	/**
	 * 批量保存明细
	 * @param adjustmentOrderId 调整单ID
	 * @param itemDTOs 明细DTO列表
	 */
	@Transactional(rollbackFor = Exception.class)
	public void batchSave(Long adjustmentOrderId, Long erpTenantId, List<AdjustmentItemDTO> itemDTOs) {
		List<AdjustmentOrderItem> items = AdjustmentItemConverter.INSTANCE.dtoListToEntityList(itemDTOs);
		for (AdjustmentOrderItem item : items) {
			item.setAdjustmentOrderId(adjustmentOrderId);
			// 货主取自单头（整单归属一个货主）
			item.setErpTenantId(erpTenantId);
		}
		this.saveBatch(items);
		log.info("Batch saved {} items for adjustmentOrderId={}", items.size(), adjustmentOrderId);
	}

	/**
	 * 根据调整单ID删除明细
	 * @param adjustmentOrderId 调整单ID
	 * @return 删除数量
	 */
	public int deleteByAdjustmentOrderId(Long adjustmentOrderId) {
		int count = baseMapper.deleteByAdjustmentOrderId(adjustmentOrderId);
		log.info("Deleted {} items for adjustmentOrderId={}", count, adjustmentOrderId);
		return count;
	}

}
