package com.erp.admin.financial.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.financial.model.qo.OrderReconciliationQO;
import com.erp.admin.financial.model.vo.OrderReconciliationDetailVO;
import com.erp.admin.financial.model.vo.OrderReconciliationStatsVO;
import com.erp.admin.financial.model.vo.OrderReconciliationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单财务对账 Mapper
 *
 * @author system
 */
@Mapper
public interface OrderReconciliationMapper {

    // ==================== 订单财务对账相关 ====================

    /**
     * 查询订单对账统计数据
     *
     * @param qo 查询条件
     * @return 统计数据
     */
    OrderReconciliationStatsVO selectOrderStats(@Param("qo") OrderReconciliationQO qo);

    /**
     * 分页查询订单财务对账列表
     *
     * @param page IPage 分页对象
     * @param qo   查询条件
     * @return 分页结果
     */
    IPage<OrderReconciliationVO> selectOrderReconciliationPage(IPage<OrderReconciliationVO> page, @Param("qo") OrderReconciliationQO qo);

    /**
     * 查询订单关联的财务记录（按 periodType 过滤）
     *
     * @param platformOrderId 平台订单号
     * @param shopId          店铺ID
     * @param periodType      报表类型
     * @return 财务记录列表
     */
    List<OrderReconciliationDetailVO.FinancialRecordItemVO> selectOrderFinancialRecords(
            @Param("platformOrderId") String platformOrderId,
            @Param("shopId") Long shopId,
            @Param("periodType") String periodType);

    /**
     * 导出订单对账数据（不分页，最多10000条）
     *
     * @param qo 查询条件
     * @return 导出数据列表
     */
    List<OrderReconciliationVO> selectOrderReconciliationForExport(@Param("qo") OrderReconciliationQO qo);


}
