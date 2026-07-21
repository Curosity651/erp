package com.erp.admin.wms.service;

import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.wms.mapper.LocationTransferItemMapper;
import com.erp.admin.wms.model.entity.LocationTransferItem;
import com.erp.admin.wms.model.vo.LocationTransferItemVO;
import lombok.RequiredArgsConstructor;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 库位调整单明细服务。
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class LocationTransferItemService
		extends ExtendServiceImpl<LocationTransferItemMapper, LocationTransferItem> {

	private final SkuBriefService skuBriefService;

	/**
	 * 按调整单ID查询明细实体。
	 * @param transferOrderId 调整单ID
	 * @return 明细列表
	 */
	public List<LocationTransferItem> getByOrderId(Long transferOrderId) {
		return baseMapper.selectByOrderId(transferOrderId);
	}

	/**
	 * 按调整单ID查询明细VO（含SKU展示信息）。
	 * @param transferOrderId 调整单ID
	 * @return 明细VO列表
	 */
	public List<LocationTransferItemVO> getVoListByOrderId(Long transferOrderId) {
		List<LocationTransferItem> items = baseMapper.selectByOrderId(transferOrderId);
		if (items == null || items.isEmpty()) {
			return new ArrayList<>();
		}
		List<LocationTransferItemVO> voList = new ArrayList<>(items.size());
		for (LocationTransferItem e : items) {
			LocationTransferItemVO vo = new LocationTransferItemVO();
			vo.setId(e.getId());
			vo.setSkuCode(e.getSkuCode());
			vo.setPhysicalInventoryId(e.getPhysicalInventoryId());
			vo.setSourceLocationCode(e.getSourceLocationCode());
			vo.setSourceQuality(e.getSourceQuality());
			vo.setTargetLocationCode(e.getTargetLocationCode());
			vo.setQuantity(e.getQuantity());
			vo.setToGood(e.getToGood());
			vo.setRemark(e.getRemark());
			voList.add(vo);
		}
		skuBriefService.enrichForQuery(voList, LocationTransferItemVO::getSkuCode, LocationTransferItemVO::setSkuBrief);
		return voList;
	}

	/**
	 * 按调整单ID删除明细。
	 * @param transferOrderId 调整单ID
	 * @return 影响行数
	 */
	public int deleteByOrderId(Long transferOrderId) {
		return baseMapper.deleteByOrderId(transferOrderId);
	}

}
