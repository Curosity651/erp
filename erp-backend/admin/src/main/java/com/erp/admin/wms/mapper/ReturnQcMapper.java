package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.qo.ReturnQO;
import com.erp.admin.wms.model.vo.ReturnOrderVO;
import org.apache.ibatis.annotations.Param;

/**
 * 退货质检读模型 Mapper（基于 wms_return_inbound_order）。平台看全部货主，仅当过滤非空时收窄。
 *
 * @author erp
 */
public interface ReturnQcMapper {

    IPage<ReturnOrderVO> pageOrders(IPage<ReturnOrderVO> page, @Param("qo") ReturnQO qo);

    ReturnOrderVO selectOrderById(@Param("id") Long id);

}
