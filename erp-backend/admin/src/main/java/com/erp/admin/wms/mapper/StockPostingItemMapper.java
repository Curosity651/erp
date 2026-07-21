package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.entity.StockPostingItem;
import com.erp.admin.wms.model.qo.StockPostingItemQO;
import com.erp.admin.wms.model.vo.PostingItemStatsVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.Collection;
import java.util.List;

/**
 * 库存过账单明细 Mapper
 *
 * @author erp
 */
public interface StockPostingItemMapper extends ExtendMapper<StockPostingItem> {

    /**
     * 根据过账单ID查询明细
     * @param postingId 过账单ID
     * @return List<StockPostingItem> 明细列表
     */
    default List<StockPostingItem> selectByPostingId(Long postingId) {
        return selectList(WrappersX.<StockPostingItem>lambdaQueryX()
                .eq(StockPostingItem::getPostingId, postingId)
                .orderByAsc(StockPostingItem::getId));
    }

    /**
     * 批量根据过账单ID查询明细
     * @param postingIds 过账单ID集合
     * @return List<StockPostingItem> 明细列表
     */
    default List<StockPostingItem> selectByPostingIds(Collection<Long> postingIds) {
        return selectList(WrappersX.<StockPostingItem>lambdaQueryX()
                .in(StockPostingItem::getPostingId, postingIds)
                .orderByAsc(StockPostingItem::getId));
    }

    /**
     * 批量统计过账单明细（SKU种类数和总数量）
     * @param postingIds 过账单ID集合
     * @return List<PostingItemStatsVO> 统计结果
     */
    @Select("<script>" +
            "SELECT posting_id as postingId, " +
            "COUNT(DISTINCT sku_code) as skuCount, " +
            "SUM(quantity) as totalQuantity " +
            "FROM wms_stock_posting_item " +
            "WHERE posting_id IN " +
            "<foreach collection='postingIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach> " +
            "GROUP BY posting_id" +
            "</script>")
    List<PostingItemStatsVO> selectStatsByPostingIds(@Param("postingIds") Collection<Long> postingIds);

    /**
     * 分页查询过账单明细
     * @param page 分页参数
     * @param qo 查询条件
     * @return IPage<StockPostingItem> 分页结果
     */
    default IPage<StockPostingItem> queryPage(IPage<StockPostingItem> page, StockPostingItemQO qo) {
        LambdaQueryWrapperX<StockPostingItem> wrapper = WrappersX.lambdaQueryX(StockPostingItem.class)
                .eq(StockPostingItem::getPostingId, qo.getPostingId())
                .likeIfPresent(StockPostingItem::getSkuCode, qo.getSkuCode())
                .inIfPresent(StockPostingItem::getErpTenantId, qo.getErpTenantIds())
                .orderByAsc(StockPostingItem::getId);
        return selectPage(page, wrapper);
    }

}
