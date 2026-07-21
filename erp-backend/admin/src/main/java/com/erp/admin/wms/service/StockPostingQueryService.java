package com.erp.admin.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.product.model.vo.SkuBriefVO;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.wms.converter.StockPostingConverter;
import com.erp.admin.wms.mapper.StockPostingItemMapper;
import com.erp.admin.wms.mapper.StockPostingMapper;
import com.erp.admin.wms.model.entity.StockPosting;
import com.erp.admin.wms.model.entity.StockPostingItem;
import com.erp.admin.wms.model.enums.PostingType;
import com.erp.admin.wms.model.qo.StockPostingItemQO;
import com.erp.admin.wms.model.qo.StockPostingQO;
import com.erp.admin.wms.model.vo.*;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 库存过账单查询服务
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class StockPostingQueryService {

    private final StockPostingMapper stockPostingMapper;
    private final StockPostingItemMapper stockPostingItemMapper;
    private final StockPostingItemService stockPostingItemService;
    private final WarehouseService warehouseService;
    private final SkuBriefService skuBriefService;
    private final ErpOwnerScopeService erpOwnerScopeService;

    /**
     * 分页查询过账单
     */
    public PageResult<StockPostingPageVO> queryPage(PageParam pageParam, StockPostingQO qo) {
        // 数据级可见性：按当前身份货主作用域收窄（货主=自身；服务商=名下货主；平台=null不过滤），忽略前端传入
        qo.setErpTenantIds(erpOwnerScopeService.readScope());
        // Mapper 返回 Entity，Service 层转换
        IPage<StockPosting> page = stockPostingMapper.prodPage(pageParam);
        stockPostingMapper.queryPage(page, qo);

        if (page.getRecords().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }

        // Entity -> VO 转换
        List<StockPostingPageVO> voList = page.getRecords().stream()
                .map(StockPostingConverter.INSTANCE::poToPageVo)
                .collect(Collectors.toList());

        // 收集需要填充的ID
        Set<Long> warehouseIds = voList.stream()
                .map(StockPostingPageVO::getWarehouseId)
                .collect(Collectors.toSet());
        Set<Long> postingIds = voList.stream()
                .map(StockPostingPageVO::getId)
                .collect(Collectors.toSet());

        // 批量查询关联数据
        Map<Long, WarehouseDisplayVO> warehouseMap = warehouseService.buildDisplayMap(warehouseIds);

        // 批量查询统计数据
        Map<Long, PostingItemStatsVO> statsMap = stockPostingItemService.getStatsByPostingIds(postingIds);

        // 填充关联数据
        for (StockPostingPageVO vo : voList) {
            vo.setWarehouseDisplay(warehouseMap.get(vo.getWarehouseId()));
            vo.setPostingTypeDesc(getPostingTypeDesc(vo.getPostingType()));

            // 填充统计数据
            PostingItemStatsVO stats = statsMap.get(vo.getId());
            if (stats != null) {
                vo.setSkuCount(stats.getSkuCount());
                vo.setTotalQuantity(stats.getTotalQuantity());
            } else {
                vo.setSkuCount(0);
                vo.setTotalQuantity(0);
            }
        }

        return new PageResult<>(voList, page.getTotal());
    }

    /**
     * 分页查询过账单明细项
     */
    public PageResult<StockPostingItemVO> queryItemPage(PageParam pageParam, StockPostingItemQO qo) {
        // 数据级可见性：按当前身份货主作用域收窄，忽略前端传入
        qo.setErpTenantIds(erpOwnerScopeService.readScope());
        IPage<StockPostingItem> page = stockPostingItemMapper.prodPage(pageParam);
        stockPostingItemMapper.queryPage(page, qo);

        if (page.getRecords().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }

        // 批量查询 SKU 展示信息
        Set<String> skuCodes = page.getRecords().stream()
                .map(StockPostingItem::getSkuCode)
                .collect(Collectors.toSet());
        Map<String, SkuBriefVO> skuBriefMap = skuBriefService.buildMapForQuery(skuCodes);

        // 转换 VO
        List<StockPostingItemVO> voList = page.getRecords().stream()
                .map(item -> {
                    StockPostingItemVO vo = StockPostingConverter.INSTANCE.itemToVo(item);
                    vo.setSkuBrief(skuBriefMap.get(item.getSkuCode()));
                    return vo;
                })
                .collect(Collectors.toList());

        return new PageResult<>(voList, page.getTotal());
    }

    /**
     * 获取过账单详情（含明细）
     */
    public StockPostingDetailVO getDetail(Long id) {
        StockPosting posting = stockPostingMapper.selectById(id);
        Assert.notNull(posting, "过账单不存在");
        // 数据级可见性：非本货主作用域不可见（平台放行；混合/未归属货主的平台作业过账头 erp 为空→货主不可见）
        Assert.isTrue(erpOwnerScopeService.canAccess(posting.getErpTenantId()), "过账单不存在");

        // 使用 Converter 转换
        StockPostingDetailVO vo = StockPostingConverter.INSTANCE.poToDetailVo(posting);
        vo.setPostingTypeDesc(getPostingTypeDesc(posting.getPostingType()));

        // 填充仓库展示信息（单个查询）
        vo.setWarehouseDisplay(warehouseService.getDisplay(posting.getWarehouseId()));

        // 查询明细
        List<StockPostingItem> items = stockPostingItemService.getByPostingId(id);

        // 批量查询 SKU 展示信息
        Set<String> skuCodes = items.stream()
                .map(StockPostingItem::getSkuCode)
                .collect(Collectors.toSet());
        Map<String, SkuBriefVO> skuBriefMap = skuBriefService.buildMapForQuery(skuCodes);

        List<StockPostingItemVO> itemVOs = items.stream()
                .map(item -> {
                    StockPostingItemVO itemVO = new StockPostingItemVO();
                    itemVO.setId(item.getId());
                    itemVO.setPostingId(item.getPostingId());
                    itemVO.setSkuCode(item.getSkuCode());
                    itemVO.setQuantity(item.getQuantity());
                    itemVO.setSkuBrief(skuBriefMap.get(item.getSkuCode()));
                    return itemVO;
                })
                .collect(Collectors.toList());

        vo.setItems(itemVOs);
        vo.setSkuCount(itemVOs.size());
        vo.setTotalQuantity(itemVOs.stream().mapToInt(StockPostingItemVO::getQuantity).sum());

        return vo;
    }

    /**
     * 获取过账类型描述
     */
    private String getPostingTypeDesc(String postingType) {
        if (postingType == null) {
            return null;
        }
        try {
            PostingType type = PostingType.valueOf(postingType);
            return type.getDescription();
        } catch (IllegalArgumentException e) {
            return postingType;
        }
    }

}
