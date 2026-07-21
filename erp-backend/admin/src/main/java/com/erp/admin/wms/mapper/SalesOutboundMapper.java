package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.SalesOutboundOrder;
import com.erp.admin.wms.model.qo.SalesOutboundQO;
import com.erp.admin.wms.model.vo.SalesOutboundDetailVO;
import com.erp.admin.wms.model.vo.SalesOutboundExportVO;
import com.erp.admin.wms.model.vo.SalesOutboundPageVO;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;

/**
 * 销售出库单 Mapper
 *
 * @author erp
 */
public interface SalesOutboundMapper extends ExtendMapper<SalesOutboundOrder> {

    /**
     * 分页查询
     * @param page 分页对象
     * @param qo 查询参数
     * @return IPage<SalesOutboundPageVO> VO分页数据
     */
    IPage<SalesOutboundPageVO> queryPage(IPage<SalesOutboundPageVO> page, @Param("qo") SalesOutboundQO qo);

    /**
     * 查询出库单详情
     * @param id 出库单ID
     * @return SalesOutboundDetailVO 出库单详情
     */
    SalesOutboundDetailVO selectDetailById(@Param("id") Long id);

    /**
     * 根据出库单号查询（用于唯一性校验）
     * @param outboundNo 出库单号
     * @param excludeId 排除的ID（编辑时使用）
     * @return SalesOutboundOrder 出库单
     */
    default SalesOutboundOrder selectByOutboundNo(String outboundNo, Long excludeId) {
        return selectOne(WrappersX.<SalesOutboundOrder>lambdaQueryX()
                .eq(SalesOutboundOrder::getOutboundNo, outboundNo)
                .neIfPresent(SalesOutboundOrder::getId, excludeId));
    }

    /**
     * 查询导出列表
     * @param qo 查询参数
     * @return List<SalesOutboundPageVO> 导出数据列表
     */
    List<SalesOutboundExportVO> selectListForExport(@Param("qo") SalesOutboundQO qo);

    /**
     * 出库单状态 CAS：仅当当前状态为 {@code expect} 时原子改为 {@code next}。
     * <p>用于签出入口消灭 TOCTOU（并发签出只有一个赢家推进）。SalesOutboundOrder 无 @Version，
     * 靠 WHERE order_status=expect 的条件更新保证幂等；调用方须校验返回值==1。
     * @return 受影响行数（1=抢占成功，0=状态已变/并发已被他人推进）
     */
    default int casOrderStatus(Long id, String expect, String next) {
        return this.update(null, WrappersX.lambdaUpdate(SalesOutboundOrder.class)
                .set(SalesOutboundOrder::getOrderStatus, next)
                .eq(SalesOutboundOrder::getId, id)
                .eq(SalesOutboundOrder::getOrderStatus, expect));
    }

}
