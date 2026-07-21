package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.qo.OutboundPickingQO;
import com.erp.admin.wms.model.vo.OutboundOrderVO;
import com.erp.admin.wms.model.vo.PickerVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 下架(拣货)读模型 Mapper：销售出库单 + 货主名/仓库名。平台看全部货主，仅当过滤非空时收窄。
 *
 * @author erp
 */
public interface OutboundPickingMapper {

    /** 分页（列表用 order.sku_count/total_quantity，不带 items） */
    IPage<OutboundOrderVO> pageOrders(IPage<OutboundOrderVO> page, @Param("qo") OutboundPickingQO qo);

    /** 单条出库单（不带 items；items 与可用量由 service 组装） */
    OutboundOrderVO selectOrderById(@Param("id") Long id);

    /** 拣货员选项（启用的系统用户） */
    List<PickerVO> selectPickers();

    /** 拣货员姓名（下架回填 picker_name） */
    String selectPickerName(@Param("id") Long id);

}
