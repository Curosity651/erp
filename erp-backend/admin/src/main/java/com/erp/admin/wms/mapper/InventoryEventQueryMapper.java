package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.qo.StockFlowQO;
import com.erp.admin.wms.model.qo.StockPostingItemQO;
import com.erp.admin.wms.model.qo.StockPostingQO;
import com.erp.admin.wms.model.vo.StockFlowDetailVO;
import com.erp.admin.wms.model.vo.StockFlowPageVO;
import com.erp.admin.wms.model.vo.StockFlowTodaySummaryVO;
import com.erp.admin.wms.model.vo.StockFlowTrendVO;
import com.erp.admin.wms.model.vo.StockPostingDetailVO;
import com.erp.admin.wms.model.vo.StockPostingItemVO;
import com.erp.admin.wms.model.vo.StockPostingPageVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@InterceptorIgnore(tenantLine = "true")
public interface InventoryEventQueryMapper {
	IPage<StockFlowPageVO> queryFlowPage(IPage<StockFlowPageVO> page, @Param("qo") StockFlowQO qo);
	StockFlowDetailVO selectFlowDetail(@Param("id") Long id, @Param("ownerIds") List<Long> ownerIds);
	List<String> listEventTypes(@Param("warehouseId") Long warehouseId, @Param("skuCode") String skuCode,
			@Param("ownerIds") List<Long> ownerIds);
	StockFlowTodaySummaryVO selectTodaySummary(@Param("warehouseId") Long warehouseId,
			@Param("ownerIds") List<Long> ownerIds);
	List<StockFlowTrendVO> selectTrend(@Param("days") Integer days, @Param("warehouseId") Long warehouseId,
			@Param("ownerIds") List<Long> ownerIds);

	IPage<StockPostingPageVO> queryEventPage(IPage<StockPostingPageVO> page, @Param("qo") StockPostingQO qo);
	StockPostingDetailVO selectEventDetail(@Param("id") Long id, @Param("ownerIds") List<Long> ownerIds);
	List<StockPostingItemVO> selectEventItems(@Param("eventId") Long eventId, @Param("skuCode") String skuCode,
			@Param("ownerIds") List<Long> ownerIds);
	IPage<StockPostingItemVO> queryEventItemPage(IPage<StockPostingItemVO> page,
			@Param("qo") StockPostingItemQO qo);
}
