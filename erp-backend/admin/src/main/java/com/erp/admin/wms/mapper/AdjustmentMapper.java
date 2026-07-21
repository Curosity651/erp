package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.AdjustmentOrder;
import com.erp.admin.wms.model.qo.AdjustmentQO;
import com.erp.admin.wms.model.vo.AdjustmentDetailVO;
import com.erp.admin.wms.model.vo.AdjustmentPageVO;
import com.erp.admin.wms.model.vo.AdjustmentStatsVO;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

import java.util.Collection;
import java.util.List;

/**
 * 调整单 Mapper
 *
 * @author erp
 */
public interface AdjustmentMapper extends ExtendMapper<AdjustmentOrder> {

	/**
	 * 分页查询
	 * @param page 分页对象
	 * @param qo 查询参数
	 * @return IPage<AdjustmentPageVO> VO分页数据
	 */
	IPage<AdjustmentPageVO> queryPage(IPage<AdjustmentPageVO> page, @Param("qo") AdjustmentQO qo);

	/**
	 * 查询调整单详情
	 * @param id 调整单ID
	 * @return AdjustmentDetailVO 调整单详情
	 */
	AdjustmentDetailVO selectDetailById(@Param("id") Long id);

	/**
	 * 查询今日调整单数量（用于生成单号）
	 * @param prefix 单号前缀（如：AD20260110）
	 * @return 今日调整单数量
	 */
	int countTodayOrders(@Param("prefix") String prefix);

	/**
	 * 批量查询调整单统计信息
	 * @param ids 调整单ID列表
	 * @return List<AdjustmentStatsVO> 统计信息列表
	 */
	List<AdjustmentStatsVO> selectStatsByIds(@Param("ids") Collection<Long> ids);

}
