package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.entity.FboInventorySnapshot;
import com.erp.admin.wms.model.dto.SkuQuantityDTO;
import com.erp.admin.wms.model.qo.FboInventoryQO;
import com.erp.admin.wms.model.vo.FboInventoryPageVO;
import com.erp.admin.wms.model.vo.FboInventorySummaryVO;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

import java.util.List;

public interface FboInventorySnapshotMapper extends ExtendMapper<FboInventorySnapshot> {
    IPage<FboInventoryPageVO> queryPage(IPage<FboInventoryPageVO> page,
            @Param("tenantId") Long tenantId, @Param("qo") FboInventoryQO qo);

    FboInventorySummaryVO selectSummary(@Param("tenantId") Long tenantId);

    List<SkuQuantityDTO> sumQuantityBySku(@Param("tenantId") Long tenantId,
            @Param("skuCodes") java.util.Collection<String> skuCodes);
}
