package com.erp.admin.product.mapper;

import java.util.Collections;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.product.converter.SkuMappingConverter;
import com.erp.admin.product.model.dto.SkuMappingCountDTO;
import com.erp.admin.product.model.entity.SkuMapping;
import com.erp.admin.product.model.qo.SkuMappingQO;
import com.erp.admin.product.model.vo.SkuMappingPageVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * SKU映射
 *
 * @author erp 2025-09-22 21:32:36
 */
public interface SkuMappingMapper extends ExtendMapper<SkuMapping> {

	/**
	 * 分页查询
	 *
	 * @param pageParam 分页参数
	 * @param qo        查询参数
	 * @return PageResult<SkuMappingPageVO> VO分页数据
	 */
	default PageResult<SkuMappingPageVO> queryPage(PageParam pageParam, SkuMappingQO qo) {
		IPage<SkuMapping> page = this.prodPage(pageParam);
		LambdaQueryWrapperX<SkuMapping> wrapper = WrappersX.lambdaQueryX(SkuMapping.class)
				.likeIfPresent(SkuMapping::getPlatformItemId, qo.getPlatformItemId())
				.likeIfPresent(SkuMapping::getSkuCode, qo.getSkuCode())
				.orderByDesc(SkuMapping::getId);
		this.selectPage(page, wrapper);
		IPage<SkuMappingPageVO> voPage = page.convert(SkuMappingConverter.INSTANCE::poToPageVo);
		return new PageResult<>(voPage.getRecords(), voPage.getTotal());
	}

	default List<SkuMapping> selectBySkuCode(String skuCode) {
		LambdaQueryWrapperX<SkuMapping> wrapperX = WrappersX.lambdaQueryX(SkuMapping.class).eq(SkuMapping::getSkuCode, skuCode);
		return this.selectList(wrapperX);
	}

	default SkuMapping selectByPlatformItemId(String platformItemId) {
		LambdaQueryWrapperX<SkuMapping> wrapperX = WrappersX.lambdaQueryX(SkuMapping.class)
				.eq(SkuMapping::getPlatformItemId, platformItemId);
		return this.selectOne(wrapperX);
	}

	/**
	 * 批量查询（platformItemId IN ...）
	 * @param platformItemIds 平台商品ID列表
	 * @return SKU映射列表
	 */
	default List<SkuMapping> selectByPlatformItemIds(List<String> platformItemIds) {
		if (platformItemIds == null || platformItemIds.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapperX<SkuMapping> wrapperX = WrappersX.lambdaQueryX(SkuMapping.class)
				.in(SkuMapping::getPlatformItemId, platformItemIds);
		return this.selectList(wrapperX);
	}

	default void deleteBySkuCode(String skuCode) {
		LambdaQueryWrapperX<SkuMapping> wrapperX = WrappersX.lambdaQueryX(SkuMapping.class).eq(SkuMapping::getSkuCode, skuCode);
		this.delete(wrapperX);
	}

	/**
	 * 批量统计SKU编码对应的映射数量
	 *
	 * @param skuCodes SKU编码列表
	 * @return SKU映射数量统计列表
	 */
	List<SkuMappingCountDTO> countBySkuCodes(@Param("skuCodes") List<String> skuCodes);

	/**
	 * 根据 SKU 编码列表查询对应的 platform_item_id 列表
	 * <p>
	 * 仅查询 platform_item_id 字段，避免查询整个实体
	 *
	 * @param skuCodes SKU 编码列表
	 * @return platform_item_id 列表（即 article）
	 */
	@Select("<script>" +
			"SELECT DISTINCT platform_item_id FROM sku_mapping " +
			"WHERE sku_code IN " +
			"<foreach collection='skuCodes' item='code' open='(' separator=',' close=')'>" +
			"#{code}" +
			"</foreach>" +
			"</script>")
	List<String> selectPlatformItemIdsBySkuCodes(@Param("skuCodes") List<String> skuCodes);

}