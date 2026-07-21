package com.erp.admin.product.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.product.converter.CategoryConverter;
import com.erp.admin.product.model.entity.Category;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.model.qo.CategoryQO;
import com.erp.admin.product.model.vo.CategoryPageVO;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * 商品品类
 *
 * @author ballcat 2025-07-26 22:36:14
 */
public interface CategoryMapper extends ExtendMapper<Category> {

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param qo 查询参数
	 * @return PageResult<CategoryPageVO> VO分页数据
	 */
	default PageResult<CategoryPageVO> queryPage(PageParam pageParam, CategoryQO qo) {
		IPage<Category> page = this.prodPage(pageParam);
		LambdaQueryWrapperX<Category> wrapper = WrappersX.lambdaQueryX(Category.class)
			.likeIfPresent(Category::getName, qo.getName())
			.likeIfPresent(Category::getCode, qo.getCode())
			.eqIfPresent(Category::getParentId, qo.getParentId())
			.eqIfPresent(Category::getStatus, qo.getStatus())
			.orderByAsc(Category::getLevel)
			.orderByAsc(Category::getSort)
			.orderByDesc(Category::getCreateTime, Category::getId);
		this.selectPage(page, wrapper);
		IPage<CategoryPageVO> voPage = page.convert(CategoryConverter.INSTANCE::poToPageVo);
		return new PageResult<>(voPage.getRecords(), voPage.getTotal());
	}

	/**
	 * 查询所有数据（用于树形结构）
	 * @param qo 查询参数
	 * @return List<CategoryPageVO> 所有数据
	 */
	default List<CategoryPageVO> queryList(CategoryQO qo) {
		LambdaQueryWrapperX<Category> wrapper = WrappersX.lambdaQueryX(Category.class)
			.likeIfPresent(Category::getName, qo.getName())
			.likeIfPresent(Category::getCode, qo.getCode())
			.eqIfPresent(Category::getParentId, qo.getParentId())
			.eqIfPresent(Category::getStatus, qo.getStatus())
			.orderByAsc(Category::getLevel)
			.orderByAsc(Category::getSort)
			.orderByDesc(Category::getCreateTime, Category::getId);
		List<Category> list = this.selectList(wrapper);
		return list.stream().map(CategoryConverter.INSTANCE::poToPageVo).collect(java.util.stream.Collectors.toList());
	}

	/**
	 * 查询顶级品类（parentId为0或null）
	 * @return List<Category> 顶级品类列表
	 */
	default List<Category> selectTopCategories() {
		LambdaQueryWrapperX<Category> wrapper = WrappersX.lambdaQueryX(Category.class)
			.eq(Category::getParentId, 0)
			.or()
			.isNull(Category::getParentId)
			.orderByAsc(Category::getSort);
		return this.selectList(wrapper);
	}

	/**
	 * 根据父品类ID查询子品类
	 * @param parentId 父品类ID
	 * @return List<Category> 子品类列表
	 */
	default List<Category> selectByParentId(Long parentId) {
		LambdaQueryWrapperX<Category> wrapper = WrappersX.lambdaQueryX(Category.class)
			.eq(Category::getParentId, parentId)
			.orderByAsc(Category::getSort);
		return this.selectList(wrapper);
	}

	/**
	 * 根据编码查询品类（用于唯一性检查）
	 * @param code 品类编码
	 * @return Category 品类实体
	 */
	default Category selectByCode(String code) {
		LambdaQueryWrapperX<Category> wrapper = WrappersX.lambdaQueryX(Category.class).eq(Category::getCode, code);
		return this.selectOne(wrapper);
	}

	/**
	 * 根据编码查询品类（排除指定ID，用于更新时的唯一性检查）
	 * @param code 品类编码
	 * @param excludeId 排除的品类ID
	 * @return Category 品类实体
	 */
	default Category selectByCodeExcludeId(String code, Long excludeId) {
		LambdaQueryWrapperX<Category> wrapper = WrappersX.lambdaQueryX(Category.class)
			.eq(Category::getCode, code)
			.ne(Category::getId, excludeId);
		return this.selectOne(wrapper);
	}

	/**
	 * 统计指定父品类下的子品类数量
	 * @param parentId 父品类ID
	 * @return Long 子品类数量
	 */
	default Long countByParentId(Long parentId) {
		LambdaQueryWrapperX<Category> wrapper = WrappersX.lambdaQueryX(Category.class)
			.eq(Category::getParentId, parentId);
		return this.selectCount(wrapper);
	}

}
