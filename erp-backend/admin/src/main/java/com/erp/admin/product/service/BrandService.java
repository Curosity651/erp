package com.erp.admin.product.service;

import java.util.List;

import com.erp.admin.product.mapper.BrandMapper;
import com.erp.admin.product.model.entity.Brand;
import com.erp.admin.product.model.qo.BrandQO;
import com.erp.admin.product.model.vo.BrandListVO;
import com.erp.admin.product.model.vo.BrandPageVO;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 品牌管理
 *
 * @author ballcat 2025-07-26 22:36:14
 */
@Service
public class BrandService extends ExtendServiceImpl<BrandMapper, Brand> {

	/**
	 * 根据QueryObeject查询分页数据
	 * @param pageParam 分页参数
	 * @param qo 查询参数对象
	 * @return PageResult<BrandPageVO> 分页数据
	 */
	public PageResult<BrandPageVO> queryPage(PageParam pageParam, BrandQO qo) {
		return baseMapper.queryPage(pageParam, qo);
	}

	/**
	 * 获取品牌列表（用于下拉选择）
	 * @return List<BrandListVO> 品牌列表
	 */
	public List<BrandListVO> getBrandList() {
		return baseMapper.selectBrandList();
	}

}
