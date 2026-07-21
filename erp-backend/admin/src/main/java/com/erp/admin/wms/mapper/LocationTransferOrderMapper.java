package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.entity.LocationTransferOrder;
import com.erp.admin.wms.model.qo.LocationTransferQO;
import com.erp.admin.wms.model.vo.LocationTransferDetailVO;
import com.erp.admin.wms.model.vo.LocationTransferPageVO;
import com.erp.admin.wms.model.vo.LocationTransferStatsVO;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

import java.util.Collection;
import java.util.List;

/**
 * 库位调整单 Mapper。
 *
 * @author erp
 */
public interface LocationTransferOrderMapper extends ExtendMapper<LocationTransferOrder> {

	/**
	 * 分页查询（联表出所属服务商/货主/仓库名）。
	 * @param page 分页对象
	 * @param qo 查询参数
	 * @return VO 分页数据
	 */
	IPage<LocationTransferPageVO> queryPage(IPage<LocationTransferPageVO> page, @Param("qo") LocationTransferQO qo);

	/**
	 * 查询详情。
	 * @param id 调整单ID
	 * @return 详情VO
	 */
	LocationTransferDetailVO selectDetailById(@Param("id") Long id);

	/**
	 * 查询今日调整单数量（用于生成单号）。
	 * @param prefix 单号前缀（如 LT20260706）
	 * @return 今日数量
	 */
	int countTodayOrders(@Param("prefix") String prefix);

	/**
	 * 批量查询统计信息（明细行数、移动总数量）。
	 * @param ids 调整单ID集合
	 * @return 统计列表
	 */
	List<LocationTransferStatsVO> selectStatsByIds(@Param("ids") Collection<Long> ids);

}
