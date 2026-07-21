package com.erp.admin.product.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.product.converter.BrandConverter;
import com.erp.admin.product.model.entity.Brand;
import com.erp.admin.product.model.qo.BrandQO;
import com.erp.admin.product.model.vo.BrandListVO;
import com.erp.admin.product.model.vo.BrandPageVO;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * 品牌管理
 *
 * @author ballcat 2025-07-26 22:36:14
 */
public interface BrandMapper extends ExtendMapper<Brand> {

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param qo 查询参数
	 * @return PageResult<BrandPageVO> VO分页数据
	 */
	default PageResult<BrandPageVO> queryPage(PageParam pageParam, BrandQO qo) {
		IPage<Brand> page = this.prodPage(pageParam);
		LambdaQueryWrapperX<Brand> wrapper = WrappersX.lambdaQueryX(Brand.class)
			.likeIfPresent(Brand::getName, qo.getName())
			.likeIfPresent(Brand::getCode, qo.getCode())
			.likeIfPresent(Brand::getOriginCountry, qo.getOriginCountry())
			.eqIfPresent(Brand::getStatus, qo.getStatus())
			.orderByDesc(Brand::getId);
		this.selectPage(page, wrapper);
		IPage<BrandPageVO> voPage = page.convert(BrandConverter.INSTANCE::poToPageVo);
		return new PageResult<>(voPage.getRecords(), voPage.getTotal());
	}

	/**
	 * 获取品牌列表（用于下拉选择）
	 * @return List<BrandListVO> 品牌列表
	 */
	default List<BrandListVO> selectBrandList() {
		LambdaQueryWrapperX<Brand> wrapper = WrappersX.lambdaQueryX(Brand.class)
			.select(Brand::getId, Brand::getName, Brand::getCode, Brand::getStatus)
			.eq(Brand::getStatus, 1) // 只查询启用状态的品牌
			.orderByAsc(Brand::getName); // 按名称排序

		List<Brand> brands = this.selectList(wrapper);

		return brands.stream().map(brand -> {
			BrandListVO vo = new BrandListVO();
			vo.setId(brand.getId());
			vo.setName(brand.getName());
			vo.setCode(brand.getCode());
			vo.setStatus(brand.getStatus());
			return vo;
		}).collect(java.util.stream.Collectors.toList());
	}

}
