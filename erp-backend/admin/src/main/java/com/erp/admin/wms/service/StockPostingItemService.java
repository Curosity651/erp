package com.erp.admin.wms.service;

import com.erp.admin.wms.mapper.StockPostingItemMapper;
import com.erp.admin.wms.model.entity.StockPostingItem;
import lombok.RequiredArgsConstructor;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.erp.admin.wms.model.vo.PostingItemStatsVO;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 库存过账单明细服务
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class StockPostingItemService extends ExtendServiceImpl<StockPostingItemMapper, StockPostingItem> {

    /**
     * 根据过账单ID查询明细
     */
    public List<StockPostingItem> getByPostingId(Long postingId) {
        return baseMapper.selectByPostingId(postingId);
    }

    /**
     * 批量根据过账单ID查询明细
     * @param postingIds 过账单ID集合
     * @return Map<postingId, List<StockPostingItem>>
     */
    public Map<Long, List<StockPostingItem>> getByPostingIds(Collection<Long> postingIds) {
        if (CollectionUtils.isEmpty(postingIds)) {
            return Collections.emptyMap();
        }
        List<StockPostingItem> items = baseMapper.selectByPostingIds(postingIds);
        return items.stream()
                .collect(Collectors.groupingBy(StockPostingItem::getPostingId));
    }

    /**
     * 批量获取过账单明细统计信息
     * @param postingIds 过账单ID集合
     * @return Map<postingId, PostingItemStatsVO>
     */
    public Map<Long, PostingItemStatsVO> getStatsByPostingIds(Collection<Long> postingIds) {
        if (CollectionUtils.isEmpty(postingIds)) {
            return Collections.emptyMap();
        }
        List<PostingItemStatsVO> statsList = baseMapper.selectStatsByPostingIds(postingIds);
        return statsList.stream()
                .collect(Collectors.toMap(PostingItemStatsVO::getPostingId, Function.identity()));
    }

}
