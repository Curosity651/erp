package com.erp.admin.system.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.system.converter.ProjectGroupConverter;
import com.erp.admin.system.model.entity.ProjectGroup;
import com.erp.admin.system.model.qo.ProjectGroupQO;
import com.erp.admin.system.model.vo.ProjectGroupListVO;
import com.erp.admin.system.model.vo.ProjectGroupPageVO;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * 项目组管理表
 *
 * @author ballcat 2025-07-26 15:27:25
 */
public interface ProjectGroupMapper extends ExtendMapper<ProjectGroup> {

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param qo 查询参数
	 * @return PageResult<ProjectGroupPageVO> VO分页数据
	 */
	default PageResult<ProjectGroupPageVO> queryPage(PageParam pageParam, ProjectGroupQO qo) {
		IPage<ProjectGroup> page = this.prodPage(pageParam);
		LambdaQueryWrapperX<ProjectGroup> wrapper = WrappersX.lambdaQueryX(ProjectGroup.class)
			.likeIfPresent(ProjectGroup::getName, qo.getName())
			.eqIfPresent(ProjectGroup::getCode, qo.getCode())
			.eqIfPresent(ProjectGroup::getStatus, qo.getStatus())
			.orderByDesc(ProjectGroup::getId);
		this.selectPage(page, wrapper);
		IPage<ProjectGroupPageVO> voPage = page.convert(ProjectGroupConverter.INSTANCE::poToPageVo);
		return new PageResult<>(voPage.getRecords(), voPage.getTotal());
	}

	/**
	 * 获取项目组列表（用于下拉选择）
	 * @return List<ProjectGroupListVO> 项目组列表
	 */
	default List<ProjectGroupListVO> selectProjectGroupList() {
		LambdaQueryWrapperX<ProjectGroup> wrapper = WrappersX.lambdaQueryX(ProjectGroup.class)
			.select(ProjectGroup::getId, ProjectGroup::getName, ProjectGroup::getCode, ProjectGroup::getStatus)
			.eq(ProjectGroup::getStatus, 1) // 只查询启用状态的项目组
			.orderByAsc(ProjectGroup::getName); // 按名称排序

		List<ProjectGroup> projectGroups = this.selectList(wrapper);

		return projectGroups.stream().map(group -> {
			ProjectGroupListVO vo = new ProjectGroupListVO();
			vo.setId(group.getId());
			vo.setName(group.getName());
			vo.setCode(group.getCode());
			vo.setStatus(group.getStatus());
			return vo;
		}).collect(java.util.stream.Collectors.toList());
	}

}
