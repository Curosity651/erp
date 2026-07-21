package com.erp.admin.system.service;

import java.util.List;

import com.erp.admin.system.mapper.ProjectGroupMapper;
import com.erp.admin.system.model.entity.ProjectGroup;
import com.erp.admin.system.model.qo.ProjectGroupQO;
import com.erp.admin.system.model.vo.ProjectGroupListVO;
import com.erp.admin.system.model.vo.ProjectGroupPageVO;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 项目组管理表
 *
 * @author ballcat 2025-07-26 15:27:25
 */
@Service
public class ProjectGroupService extends ExtendServiceImpl<ProjectGroupMapper, ProjectGroup> {

	/**
	 * 根据QueryObeject查询分页数据
	 * @param pageParam 分页参数
	 * @param qo 查询参数对象
	 * @return PageResult<ProjectGroupPageVO> 分页数据
	 */
	public PageResult<ProjectGroupPageVO> queryPage(PageParam pageParam, ProjectGroupQO qo) {
		return baseMapper.queryPage(pageParam, qo);
	}

	/**
	 * 获取项目组列表（用于下拉选择）
	 * @return List<ProjectGroupListVO> 项目组列表
	 */
	public List<ProjectGroupListVO> getProjectGroupList() {
		return baseMapper.selectProjectGroupList();
	}

}
