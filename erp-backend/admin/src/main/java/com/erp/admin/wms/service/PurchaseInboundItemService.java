package com.erp.admin.wms.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.wms.converter.PurchaseInboundItemConverter;
import com.erp.admin.wms.mapper.PurchaseInboundItemMapper;
import com.erp.admin.wms.model.dto.CustomReturnItemDTO;
import com.erp.admin.wms.model.dto.ManualInboundItemDTO;
import com.erp.admin.wms.model.dto.PurchaseInboundItemDTO;
import com.erp.admin.wms.model.entity.PurchaseInboundOrderItem;
import com.erp.admin.wms.model.vo.InboundPurchaseOrderNoVO;
import com.erp.admin.wms.model.vo.PurchaseInboundItemVO;
import com.erp.admin.wms.model.vo.PurchaseInboundStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 采购入库单明细服务
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseInboundItemService extends ExtendServiceImpl<PurchaseInboundItemMapper, PurchaseInboundOrderItem> {

	private final SkuBriefService skuBriefService;

    /**
     * 根据入库单ID查询明细列表
     * @param inboundOrderId 入库单ID
     * @return 明细列表
     */
    public List<PurchaseInboundOrderItem> getByInboundOrderId(Long inboundOrderId) {
        return baseMapper.selectByInboundOrderId(inboundOrderId);
    }

    /**
     * 根据入库单ID查询明细VO列表
     * @param inboundOrderId 入库单ID
     * @return 明细VO列表
     */
    public List<PurchaseInboundItemVO> getVoListByInboundOrderId(Long inboundOrderId) {
		List<PurchaseInboundItemVO> itemVOS = baseMapper.selectItemVOsByInboundOrderId(inboundOrderId);
		this.skuBriefService.enrichForQuery(itemVOS, PurchaseInboundItemVO::getSkuCode, PurchaseInboundItemVO::setSkuBrief);
		return itemVOS;
    }

    /**
     * 根据入库单ID删除明细
     * @param inboundOrderId 入库单ID
     */
    public void deleteByInboundOrderId(Long inboundOrderId) {
        baseMapper.deleteByInboundOrderId(inboundOrderId);
    }

    /**
     * 批量保存明细
     * @param inboundOrderId 入库单ID
     * @param items 明细DTO列表
     */
	@Transactional(rollbackFor = Exception.class)
    public void batchSave(Long inboundOrderId, List<PurchaseInboundItemDTO> items) {
        List<PurchaseInboundOrderItem> entities = PurchaseInboundItemConverter.INSTANCE.dtoListToEntityList(items);
        for (PurchaseInboundOrderItem entity : entities) {
            entity.setInboundOrderId(inboundOrderId);
        }
        this.saveBatch(entities);
    }

    /**
     * 批量保存自定义入库明细（无采购/物流关联，关联列以 0 占位，实收待平台收货时回填）
     * @param inboundOrderId 入库单ID
     * @param items 自定义明细DTO列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveManualItems(Long inboundOrderId, List<ManualInboundItemDTO> items) {
        List<PurchaseInboundOrderItem> entities = items.stream().map(dto -> {
            PurchaseInboundOrderItem entity = new PurchaseInboundOrderItem();
            entity.setInboundOrderId(inboundOrderId);
            entity.setShippingOrderItemId(0L);
            entity.setPurchaseOrderId(0L);
            entity.setPurchaseOrderItemId(0L);
            entity.setSkuCode(dto.getSkuCode());
            entity.setExpectedQuantity(dto.getExpectedQuantity());
            entity.setActualQuantity(0);
            entity.setRemark(dto.getRemark());
            return entity;
        }).collect(Collectors.toList());
        this.saveBatch(entities);
    }

    /**
     * 批量保存自定义退货明细（无采购/物流关联，关联列以 0 占位，实收待平台收货时回填；货品预判默认良品）
     * @param inboundOrderId 退货单ID
     * @param items 自定义退货明细DTO列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveCustomReturnItems(Long inboundOrderId, List<CustomReturnItemDTO> items) {
        List<PurchaseInboundOrderItem> entities = items.stream().map(dto -> {
            PurchaseInboundOrderItem entity = new PurchaseInboundOrderItem();
            entity.setInboundOrderId(inboundOrderId);
            entity.setShippingOrderItemId(0L);
            entity.setPurchaseOrderId(0L);
            entity.setPurchaseOrderItemId(0L);
            entity.setSkuCode(dto.getSkuCode());
            entity.setExpectedQuantity(dto.getExpectedQuantity());
            entity.setActualQuantity(0);
            // 货品预判可空，默认良品
            entity.setExpectedQuality(dto.getExpectedQuality() == null || dto.getExpectedQuality().isEmpty()
                    ? "GOOD" : dto.getExpectedQuality());
            entity.setRemark(dto.getRemark());
            return entity;
        }).collect(Collectors.toList());
        this.saveBatch(entities);
    }

    /**
     * 批量查询入库单统计信息
     * @param orderIds 入库单ID列表
     * @return 统计信息Map（key: 入库单ID）
     */
    public Map<Long, PurchaseInboundStatsVO> getStatsByOrderIds(List<Long> orderIds) {
        return baseMapper.selectItemSummaryByOrderIds(orderIds)
                .stream()
                .collect(Collectors.toMap(PurchaseInboundStatsVO::getInboundOrderId, v -> v));
    }

    /**
     * 批量查询入库单关联的采购单号
     * @param orderIds 入库单ID列表
     * @return 采购单号Map（key: 入库单ID）
     */
    public Map<Long, List<String>> getPurchaseOrderNosByOrderIds(List<Long> orderIds) {
        return baseMapper.selectPurchaseOrderNosByOrderIds(orderIds)
                .stream()
                .collect(Collectors.groupingBy(
                        InboundPurchaseOrderNoVO::getInboundOrderId,
                        Collectors.mapping(InboundPurchaseOrderNoVO::getPurchaseOrderNo, Collectors.toList())
                ));
    }

}
