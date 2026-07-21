package com.erp.admin.product.mapper;

import java.util.List;

import com.erp.admin.product.model.entity.SkuFileTypeConfig;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * SKU文件类型配置表
 *
 * @author ballcat 2025-08-02
 */
public interface SkuFileTypeConfigMapper extends ExtendMapper<SkuFileTypeConfig> {

	/**
	 * 查询所有启用的文件类型配置
	 * @return 文件类型配置列表
	 */
	default List<SkuFileTypeConfig> selectAllEnabled() {
		LambdaQueryWrapperX<SkuFileTypeConfig> wrapper = WrappersX.lambdaQueryX(SkuFileTypeConfig.class)
			.eq(SkuFileTypeConfig::getStatus, 1)
			.orderByAsc(SkuFileTypeConfig::getSortOrder);
		return this.selectList(wrapper);
	}

	/**
	 * 根据文件分类查询配置
	 * @param fileCategory 文件分类
	 * @return 文件类型配置列表
	 */
	default List<SkuFileTypeConfig> selectByFileCategory(String fileCategory) {
		LambdaQueryWrapperX<SkuFileTypeConfig> wrapper = WrappersX.lambdaQueryX(SkuFileTypeConfig.class)
			.eq(SkuFileTypeConfig::getFileCategory, fileCategory)
			.eq(SkuFileTypeConfig::getStatus, 1)
			.orderByAsc(SkuFileTypeConfig::getSortOrder);
		return this.selectList(wrapper);
	}

	/**
	 * 根据文件类型查询配置
	 * @param fileType 文件类型
	 * @return 文件类型配置
	 */
	default SkuFileTypeConfig selectByFileType(String fileType) {
		LambdaQueryWrapperX<SkuFileTypeConfig> wrapper = WrappersX.lambdaQueryX(SkuFileTypeConfig.class)
			.eq(SkuFileTypeConfig::getFileType, fileType)
			.eq(SkuFileTypeConfig::getStatus, 1);
		return this.selectOne(wrapper);
	}

	/**
	 * 根据文件分类和类型查询配置
	 * @param fileCategory 文件分类
	 * @param fileType 文件类型
	 * @return 文件类型配置
	 */
	default SkuFileTypeConfig selectByFileCategoryAndType(String fileCategory, String fileType) {
		LambdaQueryWrapperX<SkuFileTypeConfig> wrapper = WrappersX.lambdaQueryX(SkuFileTypeConfig.class)
			.eq(SkuFileTypeConfig::getFileCategory, fileCategory)
			.eq(SkuFileTypeConfig::getFileType, fileType)
			.eq(SkuFileTypeConfig::getStatus, 1);
		return this.selectOne(wrapper);
	}

}
