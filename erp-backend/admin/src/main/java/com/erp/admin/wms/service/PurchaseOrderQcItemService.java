package com.erp.admin.wms.service;

import com.erp.admin.wms.mapper.PurchaseOrderQcItemMapper;
import com.erp.admin.wms.model.entity.PurchaseOrderQcItem;
import com.erp.admin.wms.model.vo.PurchaseOrderQcItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 质检数据服务
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderQcItemService extends ExtendServiceImpl<PurchaseOrderQcItemMapper, PurchaseOrderQcItem> {

	/**
	 * 获取采购单质检数据列表
	 * @param purchaseOrderId 采购单ID
	 * @return 质检数据VO列表
	 */
	public List<PurchaseOrderQcItemVO> getQcItemList(Long purchaseOrderId) {
		return baseMapper.selectVoListByPurchaseOrderId(purchaseOrderId);
	}

	/**
	 * 删除采购单的所有质检数据
	 * @param purchaseOrderId 采购单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void deleteByPurchaseOrderId(Long purchaseOrderId) {
		int count = baseMapper.deleteByPurchaseOrderId(purchaseOrderId);
		log.info("Deleted {} QC items for purchase order, purchaseOrderId={}", count, purchaseOrderId);
	}

}
