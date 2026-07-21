package com.erp.admin.system.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.system.converter.PositionConverter;
import com.erp.admin.system.model.entity.Position;
import com.erp.admin.system.model.qo.PositionQO;
import com.erp.admin.system.model.vo.PositionPageVO;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * 岗位管理表
 *
 * @author ballcat 2025-07-26 15:27:26
 */
public interface PositionMapper extends ExtendMapper<Position> {

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param qo 查询参数
	 * @return PageResult<PositionPageVO> VO分页数据
	 */
	default PageResult<PositionPageVO> queryPage(PageParam pageParam, PositionQO qo) {
		IPage<Position> page = this.prodPage(pageParam);
		LambdaQueryWrapperX<Position> wrapper = WrappersX.lambdaQueryX(Position.class)
			.likeIfPresent(Position::getName, qo.getName())
			.likeIfPresent(Position::getCode, qo.getCode());
		this.selectPage(page, wrapper);
		IPage<PositionPageVO> voPage = page.convert(PositionConverter.INSTANCE::poToPageVo);
		return new PageResult<>(voPage.getRecords(), voPage.getTotal());
	}

}
