package com.erp.admin.product.mapper;

import java.util.Collection;
import java.util.List;

import com.erp.admin.product.model.entity.SkuFiles;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * SKU文件表
 *
 * @author ballcat 2025-08-02
 */
public interface SkuFilesMapper extends ExtendMapper<SkuFiles> {

	/**
	 * 根据SKU ID查询文件列表，按文件类型和排序顺序排序
	 * @param skuId SKU ID
	 * @return 文件列表
	 */
	default List<SkuFiles> selectBySkuId(Long skuId) {
		LambdaQueryWrapperX<SkuFiles> wrapper = WrappersX.lambdaQueryX(SkuFiles.class)
			.eq(SkuFiles::getSkuId, skuId)
			.orderByAsc(SkuFiles::getFileType)
			.orderByAsc(SkuFiles::getSortOrder);
		return this.selectList(wrapper);
	}

	/**
	 * 根据SKU ID和文件类型查询文件列表
	 * @param skuId SKU ID
	 * @param fileTypes 文件类型列表
	 * @return 文件列表
	 */
	default List<SkuFiles> selectBySkuIdAndFileTypes(Long skuId, Collection<String> fileTypes) {
		LambdaQueryWrapperX<SkuFiles> wrapper = WrappersX.lambdaQueryX(SkuFiles.class)
			.eq(SkuFiles::getSkuId, skuId)
			.in(SkuFiles::getFileType, fileTypes)
			.orderByAsc(SkuFiles::getSortOrder);
		return this.selectList(wrapper);
	}

	/**
	 * 根据SKU IDs和文件类型查询文件列表
	 * @param skuIds SKU IDs
	 * @param fileTypes 文件类型列表
	 * @return 文件列表
	 */
	default List<SkuFiles> selectBySkuIdsAndFileTypes(List<Long> skuIds, Collection<String> fileTypes) {
		LambdaQueryWrapperX<SkuFiles> wrapper = WrappersX.lambdaQueryX(SkuFiles.class)
				.in(SkuFiles::getSkuId, skuIds)
				.in(SkuFiles::getFileType, fileTypes)
				.orderByAsc(SkuFiles::getSortOrder);
		return this.selectList(wrapper);
	}


	/**
	 * 批量删除SKU文件
	 * @param skuId SKU ID
	 * @param fileType 文件类型（可选）
	 * @return 影响行数
	 */
	default int deleteBySkuIdAndFileType(Long skuId, String fileType) {
		LambdaQueryWrapperX<SkuFiles> wrapper = WrappersX.lambdaQueryX(SkuFiles.class).eq(SkuFiles::getSkuId, skuId);
		if (fileType != null) {
			wrapper.eq(SkuFiles::getFileType, fileType);
		}
		return this.delete(wrapper);
	}

}
