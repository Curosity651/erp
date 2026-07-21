package com.erp.admin.product.service;

import java.util.List;

import com.erp.admin.product.mapper.CategoryMapper;
import com.erp.admin.product.model.entity.Category;
import com.erp.admin.product.model.qo.CategoryQO;
import com.erp.admin.product.model.vo.CategoryHierarchyVO;
import com.erp.admin.product.model.vo.CategoryPageVO;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 商品品类
 *
 * @author ballcat 2025-07-26 22:36:14
 */
@Slf4j
@Service
public class CategoryService extends ExtendServiceImpl<CategoryMapper, Category> {

	/**
	 * 根据QueryObeject查询分页数据
	 * @param pageParam 分页参数
	 * @param qo 查询参数对象
	 * @return PageResult<CategoryPageVO> 分页数据
	 */
	public PageResult<CategoryPageVO> queryPage(PageParam pageParam, CategoryQO qo) {
		return baseMapper.queryPage(pageParam, qo);
	}

	/**
	 * 根据QueryObeject查询所有数据（用于树形结构）
	 * @param qo 查询参数对象
	 * @return List<CategoryPageVO> 所有数据
	 */
	public List<CategoryPageVO> queryList(CategoryQO qo) {
		return baseMapper.queryList(qo);
	}

	@Override
	public boolean save(Category entity) {
		log.info("保存品类前 - ID: {}, 名称: {}, 父ID: {}, 层级: {}", entity.getId(), entity.getName(), entity.getParentId(),
				entity.getLevel());

		// 检查编码唯一性
		if (baseMapper.selectByCode(entity.getCode()) != null) {
			throw new RuntimeException("品类编码已存在: " + entity.getCode());
		}

		// 自动计算层级
		calculateLevel(entity);
		log.info("保存品类后 - ID: {}, 名称: {}, 父ID: {}, 层级: {}", entity.getId(), entity.getName(), entity.getParentId(),
				entity.getLevel());
		return super.save(entity);
	}

	@Override
	public boolean updateById(Category entity) {
		log.info("更新品类前 - ID: {}, 名称: {}, 父ID: {}, 层级: {}", entity.getId(), entity.getName(), entity.getParentId(),
				entity.getLevel());

		// 检查编码唯一性（排除自己）
		if (entity.getCode() != null) {
			Category existingCategory = baseMapper.selectByCodeExcludeId(entity.getCode(), entity.getId());
			if (existingCategory != null) {
				throw new RuntimeException("品类编码已存在: " + entity.getCode());
			}
		}

		// 获取原有数据
		Category oldEntity = this.getById(entity.getId());

		// 如果父节点发生变化，需要重新计算层级
		if (oldEntity == null || !java.util.Objects.equals(oldEntity.getParentId(), entity.getParentId())) {
			// 验证不能将品类设置为自己的子品类（防止循环引用）
			if (entity.getParentId() != null && entity.getParentId() != 0L) {
				if (entity.getParentId().equals(entity.getId())) {
					throw new RuntimeException("不能将品类设置为自己的父品类");
				}
				
				// 检查新父品类是否是当前品类的子孙品类
				if (isDescendant(entity.getId(), entity.getParentId())) {
					throw new RuntimeException("不能将品类移动到自己的子品类下");
				}
			}
			
			calculateLevel(entity);
			
			// 如果父节点变化，需要更新所有子孙节点的层级
			updateDescendantsLevel(entity.getId());
		}

		log.info("更新品类后 - ID: {}, 名称: {}, 父ID: {}, 层级: {}", entity.getId(), entity.getName(), entity.getParentId(),
				entity.getLevel());
		return super.updateById(entity);
	}

	/**
	 * 根据父节点自动计算层级
	 * @param entity 品类实体
	 */
	private void calculateLevel(Category entity) {
		log.info("计算层级 - 品类ID: {}, 父品类ID: {}", entity.getId(), entity.getParentId());

		if (entity.getParentId() == null || entity.getParentId() == 0L) {
			// 顶级节点，层级为1
			entity.setLevel(1);
			// 确保顶级节点的parentId为0
			if (entity.getParentId() == null) {
				entity.setParentId(0L);
			}
			log.info("设置为顶级节点，层级: 1");
		}
		else {
			// 查询父节点
			Category parent = this.getById(entity.getParentId());
			if (parent != null) {
				// 子节点层级 = 父节点层级 + 1
				int newLevel = parent.getLevel() + 1;
				entity.setLevel(newLevel);
				log.info("父节点层级: {}, 设置子节点层级: {}", parent.getLevel(), newLevel);
			}
			else {
				// 父节点不存在，默认为顶级节点
				entity.setLevel(1);
				entity.setParentId(0L);
				log.info("父节点不存在，设置为顶级节点，层级: 1");
			}
		}
	}

	/**
	 * 批量修正所有品类的层级 用于数据修复或初始化
	 */
	public void fixAllLevels() {
		// 先处理顶级节点（parentId为0或null）
		List<Category> topCategories = baseMapper.selectTopCategories();

		for (Category category : topCategories) {
			category.setLevel(1);
			category.setParentId(0L); // 统一设置为0
			this.updateById(category);
			// 递归处理子节点
			fixChildrenLevels(category.getId(), 1);
		}
	}

	/**
	 * 递归修正子节点层级
	 * @param parentId 父节点ID
	 * @param parentLevel 父节点层级
	 */
	private void fixChildrenLevels(Long parentId, Integer parentLevel) {
		List<Category> children = baseMapper.selectByParentId(parentId);

		for (Category child : children) {
			child.setLevel(parentLevel + 1);
			this.updateById(child);
			// 递归处理子节点的子节点
			fixChildrenLevels(child.getId(), child.getLevel());
		}
	}

	/**
	 * 检查目标品类是否是当前品类的子孙品类
	 * @param categoryId 当前品类ID
	 * @param targetId 目标品类ID
	 * @return true-是子孙品类，false-不是
	 */
	private boolean isDescendant(Long categoryId, Long targetId) {
		if (categoryId == null || targetId == null) {
			return false;
		}
		
		// 获取目标品类的所有父级路径
		Category current = this.getById(targetId);
		while (current != null && current.getParentId() != null && current.getParentId() != 0L) {
			if (current.getParentId().equals(categoryId)) {
				return true;
			}
			current = this.getById(current.getParentId());
		}
		
		return false;
	}

	/**
	 * 更新所有子孙节点的层级
	 * @param categoryId 品类ID
	 */
	private void updateDescendantsLevel(Long categoryId) {
		Category category = this.getById(categoryId);
		if (category == null) {
			return;
		}
		
		List<Category> children = baseMapper.selectByParentId(categoryId);
		for (Category child : children) {
			child.setLevel(category.getLevel() + 1);
			super.updateById(child); // 使用super避免触发验证逻辑
			// 递归更新子节点的子节点
			updateDescendantsLevel(child.getId());
		}
	}

	public boolean removeById(Long id) {
		// 检查是否有子品类
		Long childCount = baseMapper.countByParentId(id);
		if (childCount > 0) {
			throw new RuntimeException("该品类下还有子品类，无法删除");
		}

		return super.removeById(id);
	}

	/**
	 * 根据编码查询品类
	 * @param code 品类编码
	 * @return Category 品类实体
	 */
	public Category getByCode(String code) {
		return baseMapper.selectByCode(code);
	}

	/**
	 * 获取指定品类的子品类列表
	 * @param parentId 父品类ID
	 * @return List<Category> 子品类列表
	 */
	public List<Category> getChildrenByParentId(Long parentId) {
		return baseMapper.selectByParentId(parentId);
	}

	/**
	 * 获取顶级品类列表
	 * @return List<Category> 顶级品类列表
	 */
	public List<Category> getTopCategories() {
		return baseMapper.selectTopCategories();
	}

	/**
	 * 获取品类的完整层级路径（从根到当前品类）
	 * @param categoryId 品类ID
	 * @return List<Category> 层级路径列表，按层级从低到高排序
	 */
	public List<Category> getCategoryPath(Long categoryId) {
		if (categoryId == null) {
			return new java.util.ArrayList<>();
		}

		List<Category> path = new java.util.ArrayList<>();
		Category current = this.getById(categoryId);

		// 从当前品类向上追溯到根品类
		while (current != null) {
			path.add(0, current); // 插入到列表开头，保证顺序是从根到叶子

			// 如果是顶级品类，停止追溯
			if (current.getParentId() == null || current.getParentId() == 0L) {
				break;
			}

			// 查询父品类
			current = this.getById(current.getParentId());
		}

		return path;
	}

	/**
	 * 构建品类层级信息VO
	 * @param categoryId 品类ID
	 * @return CategoryHierarchyVO 品类层级信息
	 */
	public CategoryHierarchyVO buildCategoryHierarchy(Long categoryId) {
		if (categoryId == null) {
			return null;
		}

		Category category = this.getById(categoryId);
		if (category == null) {
			return null;
		}

		CategoryHierarchyVO hierarchyVO = new CategoryHierarchyVO();

		// 设置当前品类信息
		hierarchyVO.setCategoryId(category.getId());
		hierarchyVO.setCategoryName(category.getName());
		hierarchyVO.setCategoryCode(category.getCode());
		hierarchyVO.setCategoryLevel(category.getLevel());

		// 获取完整路径
		List<Category> path = getCategoryPath(categoryId);

		// 构建父级路径（排除当前品类）
		List<CategoryHierarchyVO.CategoryPathNode> parentPath = new java.util.ArrayList<>();
		StringBuilder fullPathName = new StringBuilder();

		for (int i = 0; i < path.size(); i++) {
			Category pathCategory = path.get(i);

			// 添加到完整路径名称
			if (fullPathName.length() > 0) {
				fullPathName.append("/");
			}
			fullPathName.append(pathCategory.getName());

			// 如果不是最后一个（当前品类），添加到父级路径
			if (i < path.size() - 1) {
				parentPath.add(new CategoryHierarchyVO.CategoryPathNode(pathCategory.getId(), pathCategory.getName(),
						pathCategory.getCode(), pathCategory.getLevel()));
			}
		}

		hierarchyVO.setParentPath(parentPath);
		hierarchyVO.setFullPathName(fullPathName.toString());

		return hierarchyVO;
	}

}
