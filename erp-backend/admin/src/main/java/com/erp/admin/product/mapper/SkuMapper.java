package com.erp.admin.product.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.product.converter.SkuConverter;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.model.qo.SkuQO;
import com.erp.admin.product.model.qo.SkuSelectQO;
import com.erp.admin.product.model.vo.SkuPageVO;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * SKU管理表
 *
 * @author ballcat 2025-07-27 02:02:06
 */
public interface SkuMapper extends ExtendMapper<Sku> {

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param qo 查询参数
	 * @return PageResult<SkuPageVO> VO分页数据
	 */
	default PageResult<SkuPageVO> queryPage(PageParam pageParam, SkuQO qo) {
		IPage<Sku> page = this.prodPage(pageParam);
		LambdaQueryWrapperX<Sku> wrapper = getSkuLambdaQueryWrapperX(qo);

		this.selectPage(page, wrapper);
		IPage<SkuPageVO> voPage = page.convert(SkuConverter.INSTANCE::poToPageVo);
		return new PageResult<>(voPage.getRecords(), voPage.getTotal());
	}

	default LambdaQueryWrapperX<Sku> getSkuLambdaQueryWrapperX(SkuQO qo) {
		LambdaQueryWrapperX<Sku> wrapper = WrappersX.lambdaQueryX(Sku.class)
			// 基础查询条件
			.likeIfPresent(Sku::getSkuCode, qo.getSkuCode())
			.eqIfPresent(Sku::getSkuNo, qo.getSkuNo())
			.likeIfPresent(Sku::getSpuCode, qo.getSpuCode())
			.likeIfPresent(Sku::getSalesCountry, qo.getSalesCountry())
			.eqIfPresent(Sku::getCategoryId, qo.getCategoryId())
			.eqIfPresent(Sku::getProductStatus, qo.getProductStatus())
			.eqIfPresent(Sku::getShippingType, qo.getShippingType())
			.likeIfPresent(Sku::getBrandCode, qo.getBrandCode())
			.likeIfPresent(Sku::getProjectGroupCode, qo.getProjectGroupCode())
			.likeIfPresent(Sku::getSupplierCode, qo.getSupplierCode());

		// SKU名称查询（同时匹配中文名和俄文名）
		if (org.springframework.util.StringUtils.hasText(qo.getSkuName())) {
			wrapper
				.and(w -> w.like(Sku::getChineseName, qo.getSkuName()).or().like(Sku::getRussianName, qo.getSkuName()));
		}

		wrapper
			// 时间范围查询
			.geIfPresent(Sku::getCreateTime, qo.getCreateTimeStart())
			.leIfPresent(Sku::getCreateTime, qo.getCreateTimeEnd())
			// 产品属性查询
			.likeIfPresent(Sku::getPackageType, qo.getPackageType())
			.likeIfPresent(Sku::getSurfaceColor, qo.getSurfaceColor())
			.likeIfPresent(Sku::getFrameColor, qo.getFrameColor())
			.likeIfPresent(Sku::getMaterial, qo.getMaterial())
			.eqIfPresent(Sku::getHasRgbLight, qo.getHasRgbLight())
			.eqIfPresent(Sku::getHasGlass, qo.getHasGlass())
			.eqIfPresent(Sku::getNeedsPower, qo.getNeedsPower())
			.eqIfPresent(Sku::getSeasonal, qo.getSeasonal())
			// 计费重类型
			.eqIfPresent(Sku::getBillingWeightType, qo.getBillingWeightType())
			// 人员ID查询
			.eqIfPresent(Sku::getDeveloperId, qo.getDeveloperId())
			.eqIfPresent(Sku::getOperatorId, qo.getOperatorId())
			.eqIfPresent(Sku::getQcId, qo.getQualityInspectorId())
			.eqIfPresent(Sku::getPurchaserId, qo.getPurchaserId())
			.orderByDesc(Sku::getCreateTime, Sku::getId);
		return wrapper;
	}

	default List<Sku> selectListByQo(SkuQO skuQO) {
		LambdaQueryWrapperX<Sku> wrapper = getSkuLambdaQueryWrapperX(skuQO);
		return this.selectList(wrapper);
	}

	/**
	 * 根据SKU编码查询SKU（用于唯一性检查）
	 * @param skuCode SKU编码
	 * @return Sku实体
	 */
	default Sku selectBySkuCode(String skuCode) {
		LambdaQueryWrapperX<Sku> wrapper = WrappersX.lambdaQueryX(Sku.class).eq(Sku::getSkuCode, skuCode);
		return this.selectOne(wrapper);
	}

	/**
	 * 根据SKU编码查询SKU（排除指定ID，用于更新时的唯一性检查）
	 * @param skuCode SKU编码
	 * @param excludeId 排除的SKU ID
	 * @return Sku实体
	 */
	default Sku selectBySkuCodeExcludeId(String skuCode, Long excludeId) {
		LambdaQueryWrapperX<Sku> wrapper = WrappersX.lambdaQueryX(Sku.class)
			.eq(Sku::getSkuCode, skuCode)
			.ne(Sku::getId, excludeId);
		return this.selectOne(wrapper);
	}

	/**
	 * 统计指定SKU编码的数量
	 * @param skuCode SKU编码
	 * @return 数量
	 */
	default Long countBySkuCode(String skuCode) {
		LambdaQueryWrapperX<Sku> wrapper = WrappersX.lambdaQueryX(Sku.class).eq(Sku::getSkuCode, skuCode);
		return this.selectCount(wrapper);
	}

	/**
	 * 统计指定SKU编码的数量（排除指定ID）
	 * @param skuCode SKU编码
	 * @param excludeId 排除的SKU ID
	 * @return 数量
	 */
	default Long countBySkuCodeExcludeId(String skuCode, Long excludeId) {
		LambdaQueryWrapperX<Sku> wrapper = WrappersX.lambdaQueryX(Sku.class)
			.eq(Sku::getSkuCode, skuCode)
			.ne(Sku::getId, excludeId);
		return this.selectCount(wrapper);
	}

	/**
	 * 根据SKU序号查询SKU（用于唯一性检查）
	 * @param skuNo SKU序号
	 * @return Sku实体
	 */
	default Sku selectBySkuNo(Integer skuNo) {
		LambdaQueryWrapperX<Sku> wrapper = WrappersX.lambdaQueryX(Sku.class).eq(Sku::getSkuNo, skuNo);
		return this.selectOne(wrapper);
	}

	/**
	 * 根据SKU序号查询SKU（排除指定ID，用于更新时的唯一性检查）
	 * @param skuNo SKU序号
	 * @param excludeId 排除的SKU ID
	 * @return Sku实体
	 */
	default Sku selectBySkuNoExcludeId(Integer skuNo, Long excludeId) {
		LambdaQueryWrapperX<Sku> wrapper = WrappersX.lambdaQueryX(Sku.class)
			.eq(Sku::getSkuNo, skuNo)
			.ne(Sku::getId, excludeId);
		return this.selectOne(wrapper);
	}

	/**
	 * 统计指定SKU序号的数量
	 * @param skuNo SKU序号
	 * @return 数量
	 */
	default Long countBySkuNo(Integer skuNo) {
		LambdaQueryWrapperX<Sku> wrapper = WrappersX.lambdaQueryX(Sku.class).eq(Sku::getSkuNo, skuNo);
		return this.selectCount(wrapper);
	}

	/**
	 * 统计指定SKU序号的数量（排除指定ID）
	 * @param skuNo SKU序号
	 * @param excludeId 排除的SKU ID
	 * @return 数量
	 */
	default Long countBySkuNoExcludeId(Integer skuNo, Long excludeId) {
		LambdaQueryWrapperX<Sku> wrapper = WrappersX.lambdaQueryX(Sku.class)
			.eq(Sku::getSkuNo, skuNo)
			.ne(Sku::getId, excludeId);
		return this.selectCount(wrapper);
	}

	/**
	 * 根据多个SKU编码查询SKU列表
	 * @param skuCodes SKU编码列表
	 * @return SKU列表
	 */
	default List<Sku> selectBySkuCodes(Collection<String> skuCodes) {
		if (skuCodes == null || skuCodes.isEmpty()) {
			return new ArrayList<>();
		}
		LambdaQueryWrapperX<Sku> wrapper = WrappersX.lambdaQueryX(Sku.class)
			.in(Sku::getSkuCode, skuCodes)
			.orderByDesc(Sku::getId);
		return this.selectList(wrapper);
	}

	/**
	 * 根据品类ID查询SKU列表
	 * @param categoryId 品类ID
	 * @return SKU列表
	 */
	default List<Sku> selectByCategoryId(Long categoryId) {
		LambdaQueryWrapperX<Sku> wrapper = WrappersX.lambdaQueryX(Sku.class)
			.eq(Sku::getCategoryId, categoryId)
			.orderByDesc(Sku::getCreateTime, Sku::getId);
		return this.selectList(wrapper);
	}

	/**
	 * 根据产品状态查询SKU列表
	 * @param productStatus 产品状态
	 * @return SKU列表
	 */
	default List<Sku> selectByProductStatus(Integer productStatus) {
		LambdaQueryWrapperX<Sku> wrapper = WrappersX.lambdaQueryX(Sku.class)
			.eq(Sku::getProductStatus, productStatus)
			.orderByDesc(Sku::getCreateTime, Sku::getId);
		return this.selectList(wrapper);
	}

	/**
	 * 根据供应商编码查询SKU列表
	 * @param supplierCode 供应商编码
	 * @return SKU列表
	 */
	default List<Sku> selectBySupplierCode(String supplierCode) {
		LambdaQueryWrapperX<Sku> wrapper = WrappersX.lambdaQueryX(Sku.class)
			.eq(Sku::getSupplierCode, supplierCode)
			.orderByDesc(Sku::getCreateTime, Sku::getId);
		return this.selectList(wrapper);
	}

	/**
	 * 根据品牌查询SKU列表
	 * @param brand 品牌
	 * @return SKU列表
	 */
	default List<Sku> selectByBrand(String brand) {
		LambdaQueryWrapperX<Sku> wrapper = WrappersX.lambdaQueryX(Sku.class)
			.eq(Sku::getBrandCode, brand)
			.orderByDesc(Sku::getCreateTime, Sku::getId);
		return this.selectList(wrapper);
	}

	/**
	 * 高级搜索查询
	 * @param qo 查询对象
	 * @return SKU列表
	 */
	default List<Sku> selectByAdvancedSearch(SkuQO qo) {
		LambdaQueryWrapperX<Sku> wrapper = buildAdvancedSearchWrapper(qo);
		return this.selectList(wrapper);
	}

	/**
	 * 构建高级搜索条件
	 * @param qo 查询对象
	 * @return 查询条件
	 */
	default LambdaQueryWrapperX<Sku> buildAdvancedSearchWrapper(SkuQO qo) {
		LambdaQueryWrapperX<Sku> wrapper = WrappersX.lambdaQueryX(Sku.class)
			// 基础字段模糊查询
			.likeIfPresent(Sku::getSkuCode, qo.getSkuCode())
			.eqIfPresent(Sku::getSkuNo, qo.getSkuNo())
			.likeIfPresent(Sku::getSpuCode, qo.getSpuCode())
			// 精确匹配字段
			.eqIfPresent(Sku::getCategoryId, qo.getCategoryId())
			.eqIfPresent(Sku::getProductStatus, qo.getProductStatus())
			.eqIfPresent(Sku::getShippingType, qo.getShippingType())
			.eqIfPresent(Sku::getNeedsPower, qo.getNeedsPower())
			.eqIfPresent(Sku::getSeasonal, qo.getSeasonal())
			.eqIfPresent(Sku::getBillingWeightType, qo.getBillingWeightType())
			// 字符串字段模糊查询
			.likeIfPresent(Sku::getSalesCountry, qo.getSalesCountry())
			.likeIfPresent(Sku::getBrandCode, qo.getBrandCode())
			.likeIfPresent(Sku::getProjectGroupCode, qo.getProjectGroupCode())
			.likeIfPresent(Sku::getSupplierCode, qo.getSupplierCode())
			.likeIfPresent(Sku::getPackageType, qo.getPackageType())
			.likeIfPresent(Sku::getSurfaceColor, qo.getSurfaceColor())
			.likeIfPresent(Sku::getFrameColor, qo.getFrameColor())
			.likeIfPresent(Sku::getMaterial, qo.getMaterial())
			.eqIfPresent(Sku::getHasRgbLight, qo.getHasRgbLight())
			.eqIfPresent(Sku::getHasGlass, qo.getHasGlass());

		// SKU名称查询（同时匹配中文名和俄文名）
		if (org.springframework.util.StringUtils.hasText(qo.getSkuName())) {
			wrapper
				.and(w -> w.like(Sku::getChineseName, qo.getSkuName()).or().like(Sku::getRussianName, qo.getSkuName()));
		}

		return wrapper
			// 时间范围查询
			.geIfPresent(Sku::getCreateTime, qo.getCreateTimeStart())
			.leIfPresent(Sku::getCreateTime, qo.getCreateTimeEnd())
			// 人员ID查询
			.eqIfPresent(Sku::getDeveloperId, qo.getDeveloperId())
			.eqIfPresent(Sku::getOperatorId, qo.getOperatorId())
			.eqIfPresent(Sku::getQcId, qo.getQualityInspectorId())
			.eqIfPresent(Sku::getPurchaserId, qo.getPurchaserId())
			// 排序
			.orderByDesc(Sku::getCreateTime, Sku::getId);
	}

	/**
	 * 统计各状态的SKU数量
	 * @return 状态统计结果
	 */
	default List<Map<String, Object>> countByProductStatus() {
		return this
			.selectMaps(WrappersX.lambdaQueryX(Sku.class).select(Sku::getProductStatus).groupBy(Sku::getProductStatus));
	}

	/**
	 * 统计各品牌的SKU数量
	 * @return 品牌统计结果
	 */
	default List<Map<String, Object>> countByBrand() {
		return this.selectMaps(WrappersX.lambdaQueryX(Sku.class)
			.select(Sku::getBrandCode)
			.isNotNull(Sku::getBrandCode)
			.ne(Sku::getBrandCode, "")
			.groupBy(Sku::getBrandCode));
	}

	/**
	 * 统计各供应商的SKU数量
	 * @return 供应商统计结果
	 */
	default List<Map<String, Object>> countBySupplier() {
		return this.selectMaps(WrappersX.lambdaQueryX(Sku.class)
			.select(Sku::getSupplierCode)
			.isNotNull(Sku::getSupplierCode)
			.ne(Sku::getSupplierCode, "")
			.groupBy(Sku::getSupplierCode));
	}

	/**
	 * 获取所有品牌列表（去重）
	 * @return 品牌列表
	 */
	default List<String> selectDistinctBrands() {
		List<Sku> skus = this.selectList(WrappersX.lambdaQueryX(Sku.class)
			.select(Sku::getBrandCode)
			.isNotNull(Sku::getBrandCode)
			.ne(Sku::getBrandCode, "")
			.groupBy(Sku::getBrandCode)
			.orderBy(true, true, Sku::getBrandCode));
		return skus.stream().map(Sku::getBrandCode).distinct().collect(Collectors.toList());
	}

	/**
	 * 获取所有项目组列表（去重）
	 * @return 项目组列表
	 */
	default List<String> selectDistinctProjectGroups() {
		List<Sku> skus = this.selectList(WrappersX.lambdaQueryX(Sku.class)
			.select(Sku::getProjectGroupCode)
			.isNotNull(Sku::getProjectGroupCode)
			.ne(Sku::getProjectGroupCode, "")
			.groupBy(Sku::getProjectGroupCode)
			.orderBy(true, true, Sku::getProjectGroupCode));
		return skus.stream().map(Sku::getProjectGroupCode).distinct().collect(Collectors.toList());
	}

	/**
	 * 获取所有供应商编码列表（去重）
	 * @return 供应商编码列表
	 */
	default List<String> selectDistinctSupplierCodes() {
		List<Sku> skus = this.selectList(WrappersX.lambdaQueryX(Sku.class)
			.select(Sku::getSupplierCode)
			.isNotNull(Sku::getSupplierCode)
			.ne(Sku::getSupplierCode, "")
			.groupBy(Sku::getSupplierCode)
			.orderBy(true, true, Sku::getSupplierCode));
		return skus.stream().map(Sku::getSupplierCode).distinct().collect(Collectors.toList());
	}

	/**
	 * 获取所有销售国家列表（去重）
	 * @return 销售国家列表
	 */
	default List<String> selectDistinctSalesCountries() {
		List<Sku> skus = this.selectList(WrappersX.lambdaQueryX(Sku.class)
			.select(Sku::getSalesCountry)
			.isNotNull(Sku::getSalesCountry)
			.ne(Sku::getSalesCountry, "")
			.groupBy(Sku::getSalesCountry)
			.orderBy(true, true, Sku::getSalesCountry));
		return skus.stream().map(Sku::getSalesCountry).distinct().collect(Collectors.toList());
	}

	/**
	 * 覆盖更新SKU，允许将非必填字段设置为null
	 * @param updateDTO SKU更新DTO
	 * @return 更新行数
	 */
	default int updateSkuWithOverride(com.erp.admin.product.model.dto.SkuUpdateDTO updateDTO) {
		// 使用LambdaUpdateWrapper进行精确的字段更新控制
		com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Sku> updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();

		updateWrapper.eq(Sku::getId, updateDTO.getId());

		// 必填字段 - 始终更新
		updateWrapper.set(Sku::getSkuCode, updateDTO.getSkuCode());
		updateWrapper.set(Sku::getSkuNo, updateDTO.getSkuNo());
		updateWrapper.set(Sku::getSpuCode, updateDTO.getSpuCode());

		// 非必填字段 - 允许覆盖更新（包括设置为null）
		updateWrapper.set(Sku::getSalesCountry, updateDTO.getSalesCountry());
		updateWrapper.set(Sku::getCategoryId, updateDTO.getCategoryId());
		updateWrapper.set(Sku::getProductStatus, updateDTO.getProductStatus());
		updateWrapper.set(Sku::getShippingType, updateDTO.getShippingType());
		updateWrapper.set(Sku::getBrandCode, updateDTO.getBrandCode());
		updateWrapper.set(Sku::getProjectGroupCode, updateDTO.getProjectGroupCode());
		updateWrapper.set(Sku::getDescription, updateDTO.getDescription());
		updateWrapper.set(Sku::getChineseName, updateDTO.getChineseName());
		updateWrapper.set(Sku::getRussianName, updateDTO.getRussianName());
		updateWrapper.set(Sku::getPackageType, updateDTO.getPackageType());
		updateWrapper.set(Sku::getBillingWeightType, updateDTO.getBillingWeightType());
		updateWrapper.set(Sku::getRemarks, updateDTO.getRemarks());
		updateWrapper.set(Sku::getCustomsDeclarationName, updateDTO.getCustomsDeclarationName());
		updateWrapper.set(Sku::getCustomsDeclarationCode, updateDTO.getCustomsDeclarationCode());
		updateWrapper.set(Sku::getNeedsPower, updateDTO.getNeedsPower());
		updateWrapper.set(Sku::getSeasonal, updateDTO.getSeasonal());
		updateWrapper.set(Sku::getBcBoxMinBreakage, updateDTO.getBcBoxMinBreakage());
		updateWrapper.set(Sku::getPackaging, updateDTO.getPackaging());
		updateWrapper.set(Sku::getSurfaceColor, updateDTO.getSurfaceColor());
		updateWrapper.set(Sku::getFrameColor, updateDTO.getFrameColor());
		updateWrapper.set(Sku::getMaterial, updateDTO.getMaterial());
		updateWrapper.set(Sku::getHasRgbLight, updateDTO.getHasRgbLight());
		updateWrapper.set(Sku::getHasGlass, updateDTO.getHasGlass());
		updateWrapper.set(Sku::getWeight, updateDTO.getWeight());
		updateWrapper.set(Sku::getWeightUnit, updateDTO.getWeightUnit());
		updateWrapper.set(Sku::getPackageLength, updateDTO.getPackageLength());
		updateWrapper.set(Sku::getPackageWidth, updateDTO.getPackageWidth());
		updateWrapper.set(Sku::getPackageHeight, updateDTO.getPackageHeight());
		updateWrapper.set(Sku::getPackageUnit, updateDTO.getPackageUnit());
		updateWrapper.set(Sku::getOuterLengthMm, updateDTO.getOuterLengthMm());
		updateWrapper.set(Sku::getOuterWidthMm, updateDTO.getOuterWidthMm());
		updateWrapper.set(Sku::getOuterHeightMm, updateDTO.getOuterHeightMm());
		updateWrapper.set(Sku::getOuterGrossWeightG, updateDTO.getOuterGrossWeightG());
		updateWrapper.set(Sku::getQuantityPerPallet, updateDTO.getQuantityPerPallet());
		updateWrapper.set(Sku::getFunctionalRequirements, updateDTO.getFunctionalRequirements());
		updateWrapper.set(Sku::getSupplierCode, updateDTO.getSupplierCode());
		updateWrapper.set(Sku::getIncludeTax, updateDTO.getIncludeTax());
		updateWrapper.set(Sku::getTaxRate, updateDTO.getTaxRate());
		updateWrapper.set(Sku::getPurchasePrice, updateDTO.getPurchasePrice());
		updateWrapper.set(Sku::getMinimumOrderQuantity, updateDTO.getMinimumOrderQuantity());
		updateWrapper.set(Sku::getProductionCycle, updateDTO.getProductionCycle());
		updateWrapper.set(Sku::getDeveloperId, updateDTO.getDeveloperId());
		updateWrapper.set(Sku::getOperatorId, updateDTO.getOperatorId());
		updateWrapper.set(Sku::getQcId, updateDTO.getQcId());
		updateWrapper.set(Sku::getPurchaserId, updateDTO.getPurchaserId());

		return this.update(null, updateWrapper);
	}

	/**
	 * 获取SKU数据概览统计
	 * @return 统计数据
	 */
	default Map<String, Object> getSkuOverviewStats() {
		Map<String, Object> stats = new java.util.HashMap<>();

		// 总数量
		Long totalCount = this.selectCount(null);
		stats.put("totalCount", totalCount);

		// 各状态数量
		List<Map<String, Object>> statusStats = countByProductStatus();
		stats.put("statusStats", statusStats);

		// 品牌数量
		Long brandCount = this.selectCount(WrappersX.lambdaQueryX(Sku.class)
			.isNotNull(Sku::getBrandCode)
			.ne(Sku::getBrandCode, "")
			.groupBy(Sku::getBrandCode));
		stats.put("brandCount", brandCount);

		// 供应商数量
		Long supplierCount = this.selectCount(WrappersX.lambdaQueryX(Sku.class)
			.isNotNull(Sku::getSupplierCode)
			.ne(Sku::getSupplierCode, "")
			.groupBy(Sku::getSupplierCode));
		stats.put("supplierCount", supplierCount);

		return stats;
	}

	// ==================== SKU选择弹窗专用查询 ====================

	/**
	 * SKU选择弹窗分页查询（轻量级，仅返回必要字段）
	 * @param pageParam 分页参数
	 * @param qo 查询参数
	 * @return PageResult<Sku> 分页数据
	 */
	default PageResult<Sku> querySelectPage(PageParam pageParam, SkuSelectQO qo) {
		IPage<Sku> page = this.prodPage(pageParam);
		LambdaQueryWrapperX<Sku> wrapper = WrappersX.lambdaQueryX(Sku.class)
			.select(Sku::getId, Sku::getSkuCode, Sku::getChineseName, Sku::getCategoryId,
				Sku::getOuterLengthMm, Sku::getOuterWidthMm, Sku::getOuterHeightMm)
			.likeIfPresent(Sku::getSkuCode, qo.getSkuCode());

		// SKU名称查询（同时匹配中文名和俄文名）
		if (org.springframework.util.StringUtils.hasText(qo.getSkuName())) {
			wrapper.and(w -> w.like(Sku::getChineseName, qo.getSkuName())
				.or()
				.like(Sku::getRussianName, qo.getSkuName()));
		}

		wrapper.orderByDesc(Sku::getCreateTime, Sku::getId);

		this.selectPage(page, wrapper);
		return new PageResult<>(page.getRecords(), page.getTotal());
	}

}
