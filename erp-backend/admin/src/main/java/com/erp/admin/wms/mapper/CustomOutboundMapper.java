package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.entity.SalesOutboundOrder;
import com.erp.admin.wms.model.qo.CustomOutboundQO;
import com.erp.admin.wms.model.vo.CustomOutboundDetailVO;
import com.erp.admin.wms.model.vo.CustomOutboundPageVO;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

import java.util.List;

/**
 * 自定义出库单 Mapper
 * <p>
 * 与销售出库单共用 {@code wms_sales_outbound_order} 表，
 * 本 Mapper 的查询强制过滤 {@code source_type = 'CUSTOM'}。
 * </p>
 *
 * @author erp
 */
public interface CustomOutboundMapper extends ExtendMapper<SalesOutboundOrder> {

    /**
     * 分页查询（仅 CUSTOM）
     * @param page 分页对象
     * @param qo 查询参数
     * @return IPage<CustomOutboundPageVO> VO分页数据
     */
    IPage<CustomOutboundPageVO> queryPage(IPage<CustomOutboundPageVO> page, @Param("qo") CustomOutboundQO qo);

    /**
     * 查询出库单详情（仅 CUSTOM）
     * @param id 出库单ID
     * @return CustomOutboundDetailVO 出库单详情
     */
    CustomOutboundDetailVO selectDetailById(@Param("id") Long id);

    /**
     * 查询导出列表（仅 CUSTOM）
     * @param qo 查询参数
     * @return List<CustomOutboundPageVO> 导出数据列表
     */
    List<CustomOutboundPageVO> selectListForExport(@Param("qo") CustomOutboundQO qo);

}
