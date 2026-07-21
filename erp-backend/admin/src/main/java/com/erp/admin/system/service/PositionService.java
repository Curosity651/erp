package com.erp.admin.system.service;

import com.erp.admin.system.mapper.PositionMapper;
import com.erp.admin.system.model.entity.Position;
import com.erp.admin.system.model.qo.PositionQO;
import com.erp.admin.system.model.vo.PositionPageVO;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 岗位管理表
 *
 * @author ballcat 2025-07-26 15:27:26
 */
@Service
public class PositionService extends ExtendServiceImpl<PositionMapper, Position> {

	/**
	 * 根据QueryObeject查询分页数据
	 * @param pageParam 分页参数
	 * @param qo 查询参数对象
	 * @return PageResult<PositionPageVO> 分页数据
	 */
	public PageResult<PositionPageVO> queryPage(PageParam pageParam, PositionQO qo) {
		return baseMapper.queryPage(pageParam, qo);
	}

}
