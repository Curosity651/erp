package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.dto.RegionSkuStockDTO;
import com.erp.admin.wms.model.dto.WarehouseAggregateDTO;
import com.erp.admin.wms.model.qo.InventoryQO;
import com.erp.admin.wms.model.vo.InventoryDetailVO;
import com.erp.admin.wms.model.vo.InventoryPageVO;
import com.erp.admin.wms.model.vo.InventorySummaryVO;
import com.erp.admin.wms.model.vo.SkuSummaryVO;
import com.erp.admin.wms.model.vo.WarehouseSummaryVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface OwnerInventoryQueryMapper {

	InventorySummaryVO selectSummary(@Param("ownerIds") Collection<Long> ownerIds,
			@Param("unrestricted") boolean unrestricted);

	List<WarehouseSummaryVO> selectWarehouseSummary(@Param("ownerIds") Collection<Long> ownerIds,
			@Param("unrestricted") boolean unrestricted);

	IPage<SkuSummaryVO> selectSkuSummary(IPage<SkuSummaryVO> page,
			@Param("ownerIds") Collection<Long> ownerIds, @Param("unrestricted") boolean unrestricted,
			@Param("keyword") String keyword, @Param("stockStatus") String stockStatus);

	IPage<InventoryPageVO> selectLocationPage(IPage<InventoryPageVO> page,
			@Param("ownerIds") Collection<Long> ownerIds, @Param("unrestricted") boolean unrestricted,
			@Param("qo") InventoryQO qo);

	InventoryDetailVO selectDetail(@Param("ownerIds") Collection<Long> ownerIds,
			@Param("unrestricted") boolean unrestricted, @Param("warehouseId") Long warehouseId,
			@Param("skuCode") String skuCode);

	List<RegionSkuStockDTO> selectRegionSkuStocks(@Param("ownerIds") Collection<Long> ownerIds,
			@Param("unrestricted") boolean unrestricted, @Param("regionIds") Collection<Long> regionIds,
			@Param("keyword") String keyword);

	List<WarehouseAggregateDTO> selectRegionAggregates(@Param("ownerIds") Collection<Long> ownerIds,
			@Param("unrestricted") boolean unrestricted, @Param("regionIds") Collection<Long> regionIds);
}
