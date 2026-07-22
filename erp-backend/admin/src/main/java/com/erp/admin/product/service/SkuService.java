package com.erp.admin.product.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.product.converter.SkuConverter;
import com.erp.admin.product.exception.SkuBusinessException;
import com.erp.admin.product.mapper.SkuFilesMapper;
import com.erp.admin.product.mapper.SkuMapper;
import com.erp.admin.product.mapper.SkuMappingMapper;
import com.erp.admin.product.model.dto.SkuCreateDTO;
import com.erp.admin.product.model.dto.SkuFileDTO;
import com.erp.admin.product.model.dto.SkuMappingCountDTO;
import com.erp.admin.product.model.dto.SkuUpdateDTO;
import com.erp.admin.product.model.entity.Brand;
import com.erp.admin.product.model.entity.Category;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.model.entity.SkuFiles;
import com.erp.admin.product.model.qo.SkuQO;
import com.erp.admin.product.model.qo.SkuSelectQO;
import com.erp.admin.product.model.vo.*;
import com.erp.admin.system.config.OssImageStyles;
import com.erp.admin.system.config.OssBucketKeys;
import com.erp.admin.system.model.entity.ProjectGroup;
import com.erp.admin.system.service.OssService;
import com.erp.admin.system.service.ProjectGroupService;
import com.erp.admin.tenant.service.TenantIdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.business.system.model.entity.SysUser;
import org.ballcat.business.system.service.SysUserService;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * SKU管理表
 *
 * @author ballcat 2025-07-27 02:02:06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkuService extends ExtendServiceImpl<SkuMapper, Sku> {

	private final BrandService brandService;

	private final CategoryService categoryService;

	private final ProjectGroupService projectGroupService;

	private final SysUserService sysUserService;

	private final SkuFilesMapper skuFilesMapper;

	private final SkuImageResolver skuImageResolver;

	private final SkuMappingMapper skuMappingMapper;

	private final TenantIdentityService tenantIdentityService;

	private final OssService ossService;

	/**
	 * 根据QueryObeject查询分页数据
	 *
	 * @param pageParam 分页参数
	 * @param qo        查询参数对象
	 * @return PageResult<SkuPageVO> 分页数据
	 */
	public PageResult<SkuPageVO> queryPage(PageParam pageParam, SkuQO qo) {
		PageResult<SkuPageVO> pageResult = baseMapper.queryPage(pageParam, qo);

		// 填充关联字段
		fillRelatedFields(pageResult.getRecords());
		return pageResult;
	}

	/**
	 * 将SKU列表转换为导出VO列表，并按需填充图片、类目名称、体积等字段
	 *
	 * @param qo 查询条件
	 * @return 导出VO列表
	 */
	public List<SkuExportVO> buildExportVOs(SkuQO qo) {
		List<Sku> skus = this.listByQO(qo);
		if (CollectionUtils.isEmpty(skus)) {
			return Collections.emptyList();
		}

		// 批量准备辅助数据：类目名称
		List<Long> categoryIds = skus.stream()
				.map(Sku::getCategoryId)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toList());
		Map<Long, String> categoryNameMap = new HashMap<>();
		if (!CollectionUtils.isEmpty(categoryIds)) {
			List<Category> categories = this.categoryService.listByIds(categoryIds);
			if (!CollectionUtils.isEmpty(categories)) {
				categoryNameMap = categories.stream().collect(Collectors.toMap(Category::getId, Category::getName));
			}
		}

		// 批量查询图片（使用 SkuImageResolver 统一处理）
		List<Long> skuIds = skus.stream().map(Sku::getId).filter(Objects::nonNull).collect(Collectors.toList());
		Map<Long, URL> firstImageUrlMap = skuImageResolver.resolveForExportAsURL(skuIds);

		// 组装导出VO
		List<SkuExportVO> result = new ArrayList<>(skus.size());
		for (Sku sku : skus) {
			SkuExportVO vo = SkuConverter.INSTANCE.poToExportVO(sku);
			// 类目
			if (sku.getCategoryId() != null) {
				vo.setCategoryName(categoryNameMap.get(sku.getCategoryId()));
			}
			// 体积
			vo.setVolume(calcVolume(sku));
			// 图片
			URL pic = firstImageUrlMap.get(sku.getId());
			vo.setPic(pic);
			result.add(vo);
		}
		return result;
	}

	private BigDecimal calcVolume(Sku sku) {
		BigDecimal l = sku.getPackageLength();
		BigDecimal w = sku.getPackageWidth();
		BigDecimal h = sku.getPackageHeight();
		if (l == null || w == null || h == null) {
			return null;
		}
		try {
			return l.multiply(w).multiply(h).setScale(6, RoundingMode.HALF_UP);
		} catch (Exception ex) {
			return null;
		}
	}

	public List<Sku> listByQO(SkuQO skuQO) {
		return baseMapper.selectListByQo(skuQO);
	}

	/**
	 * 验证SKU编码是否唯一
	 *
	 * @param skuCode   SKU编码
	 * @param excludeId 排除的ID（更新时使用）
	 * @return true-唯一，false-重复
	 */
	public boolean validateSkuCode(String skuCode, Long excludeId) {
		if (!StringUtils.hasText(skuCode)) {
			return false;
		}

		log.debug("验证SKU编码唯一性: skuCode={}, excludeId={}", skuCode, excludeId);

		Long count;
		if (excludeId != null) {
			// 更新时排除当前记录
			count = baseMapper.countBySkuCodeExcludeId(skuCode.trim().toUpperCase(), excludeId);
		} else {
			// 新增时检查所有记录
			count = baseMapper.countBySkuCode(skuCode.trim().toUpperCase());
		}

		boolean isUnique = count == 0;
		log.debug("SKU编码唯一性验证结果: skuCode={}, count={}, isUnique={}", skuCode, count, isUnique);

		return isUnique;
	}

	/**
	 * 验证SKU序号是否唯一
	 *
	 * @param skuNo     SKU序号
	 * @param excludeId 排除的ID（更新时使用）
	 * @return true-唯一，false-重复
	 * @Deprecated skuNo 现在可能会唯一了
	 */
	@Deprecated
	public boolean validateSkuNo(Integer skuNo, Long excludeId) {
		if (skuNo == null || skuNo <= 0) {
			return false;
		}

		log.debug("验证SKU序号唯一性: skuNo={}, excludeId={}", skuNo, excludeId);

		Long count;
		if (excludeId != null) {
			// 更新时排除当前记录
			count = baseMapper.countBySkuNoExcludeId(skuNo, excludeId);
		} else {
			// 新增时检查所有记录
			count = baseMapper.countBySkuNo(skuNo);
		}

		boolean isUnique = count == 0;
		log.debug("SKU序号唯一性验证结果: skuNo={}, count={}, isUnique={}", skuNo, count, isUnique);

		return isUnique;
	}

	/**
	 * 根据SKU编码查询SKU
	 *
	 * @param skuCode SKU编码
	 * @return SKU实体
	 */
	public Sku getBySkuCode(String skuCode) {
		if (!StringUtils.hasText(skuCode)) {
			return null;
		}
		return baseMapper.selectBySkuCode(skuCode.trim().toUpperCase());
	}

	/**
	 * 根据SKU序号查询SKU
	 *
	 * @param skuNo SKU序号
	 * @return SKU实体
	 */
	public Sku getBySkuNo(Integer skuNo) {
		if (skuNo == null || skuNo <= 0) {
			return null;
		}
		return baseMapper.selectBySkuNo(skuNo);
	}

	/**
	 * 验证SKU关键属性（编码和序号的唯一性）
	 *
	 * @param entity    SKU实体
	 * @param excludeId 排除的ID（更新时使用，新增时传null）
	 */
	private void validateSkuKeyAttributes(Sku entity, Long excludeId) {
		// 验证SKU编码唯一性
		if (entity.getSkuCode() != null) {
			entity.setSkuCode(entity.getSkuCode().trim().toUpperCase());
			if (!validateSkuCode(entity.getSkuCode(), excludeId)) {
				throw SkuBusinessException.skuCodeDuplicate(entity.getSkuCode());
			}
		}
	}

	/**
	 * 验证SkuUpdateDTO的关键属性
	 *
	 * @param updateDTO 更新DTO
	 */
	private void validateSkuUpdateDTOKeyAttributes(SkuUpdateDTO updateDTO) {
		// 验证SKU编码唯一性
		if (updateDTO.getSkuCode() != null) {
			String normalizedCode = updateDTO.getSkuCode().trim().toUpperCase();
			updateDTO.setSkuCode(normalizedCode);
			if (!validateSkuCode(normalizedCode, updateDTO.getId())) {
				throw SkuBusinessException.skuCodeDuplicate(normalizedCode);
			}
		}
	}

	@Override
	public boolean save(Sku entity) {
		// 保存前验证关键属性
		validateSkuKeyAttributes(entity, null);

		log.info("创建SKU: skuCode={}, skuNo={}, spuCode={}", entity.getSkuCode(), entity.getSkuNo(),
				entity.getSpuCode());
		return super.save(entity);
	}

	@Override
	public boolean updateById(Sku entity) {
		// 更新前验证关键属性
		validateSkuKeyAttributes(entity, entity.getId());

		log.info("更新SKU: id={}, skuCode={}, skuNo={}, spuCode={}", entity.getId(), entity.getSkuCode(),
				entity.getSkuNo(), entity.getSpuCode());
		return super.updateById(entity);
	}

	/**
	 * 创建SKU
	 *
	 * @param createDTO 创建DTO
	 * @return 是否成功
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean saveSku(SkuCreateDTO createDTO) {
		// 转换DTO为实体
		Sku sku = SkuConverter.INSTANCE.createDtoToPo(createDTO);
		boolean saved = this.save(sku);

		if (saved && !CollectionUtils.isEmpty(createDTO.getFiles())) {
			// 保存文件信息
			saveSkuFilesFromMap(sku.getId(), createDTO.getFiles());
		}

		return saved;
	}

	/**
	 * 更新SKU
	 *
	 * @param updateDTO 更新DTO
	 * @return 是否成功
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean updateSku(SkuUpdateDTO updateDTO) {
		// 更新前验证关键属性（编码和序号的唯一性）
		validateSkuUpdateDTOKeyAttributes(updateDTO);

		log.info("更新SKU: id={}, skuCode={}, skuNo={}, spuCode={}", updateDTO.getId(), updateDTO.getSkuCode(),
				updateDTO.getSkuNo(), updateDTO.getSpuCode());

		// 使用Mapper的覆盖更新方法，允许将非必填字段设置为null
		int updated = baseMapper.updateSkuWithOverride(updateDTO);
		boolean success = updated > 0;

		if (success && updateDTO.getFiles() != null) {
			// 智能更新文件信息（对比后增删）
			updateSkuFilesFromMap(updateDTO.getId(), updateDTO.getFiles());
		}

		return success;
	}

	/**
	 * 根据SKU编码列表批量查询SKU信息
	 *
	 * @param skuCodes SKU编码列表
	 * @return SKU列表
	 */
	public List<Sku> listBySkuCodes(Collection<String> skuCodes) {
		if (skuCodes == null || skuCodes.isEmpty()) {
			return new ArrayList<>();
		}
		return baseMapper.selectBySkuCodes(skuCodes);
	}

	/**
	 * 根据SKU编码列表批量查询SKU信息，返回Map
	 *
	 * @param skuCodes SKU编码列表
	 * @return Map<skuCode, Sku>
	 */
	public Map<String, Sku> getSkuMapByCodes(Collection<String> skuCodes) {
		if (skuCodes == null || skuCodes.isEmpty()) {
			return Collections.emptyMap();
		}
		return baseMapper.selectBySkuCodes(skuCodes).stream()
				.collect(Collectors.toMap(Sku::getSkuCode, Function.identity(), (a, b) -> a));
	}

	/**
	 * 根据品类ID查询SKU列表
	 *
	 * @param categoryId 品类ID
	 * @return SKU列表
	 */
	public List<Sku> getByCategoryId(Long categoryId) {
		return baseMapper.selectByCategoryId(categoryId);
	}

	/**
	 * 根据产品状态查询SKU列表
	 *
	 * @param productStatus 产品状态
	 * @return SKU列表
	 */
	public List<Sku> getByProductStatus(Integer productStatus) {
		return baseMapper.selectByProductStatus(productStatus);
	}

	/**
	 * 根据供应商编码查询SKU列表
	 *
	 * @param supplierCode 供应商编码
	 * @return SKU列表
	 */
	public List<Sku> getBySupplierCode(String supplierCode) {
		return baseMapper.selectBySupplierCode(supplierCode);
	}

	/**
	 * 根据品牌查询SKU列表
	 *
	 * @param brand 品牌
	 * @return SKU列表
	 */
	public List<Sku> getByBrand(String brand) {
		return baseMapper.selectByBrand(brand);
	}

	/**
	 * 统计各品牌的SKU数量
	 *
	 * @return 品牌统计结果
	 */
	public List<Map<String, Object>> countByBrand() {
		return baseMapper.countByBrand();
	}

	/**
	 * 统计各供应商的SKU数量
	 *
	 * @return 供应商统计结果
	 */
	public List<Map<String, Object>> countBySupplier() {
		return baseMapper.countBySupplier();
	}

	/**
	 * 获取所有品牌列表（去重）
	 *
	 * @return 品牌列表
	 */
	public List<String> getDistinctBrands() {
		return baseMapper.selectDistinctBrands();
	}

	/**
	 * 获取所有项目组列表（去重）
	 *
	 * @return 项目组列表
	 */
	public List<String> getDistinctProjectGroups() {
		return baseMapper.selectDistinctProjectGroups();
	}

	/**
	 * 获取所有供应商编码列表（去重）
	 *
	 * @return 供应商编码列表
	 */
	public List<String> getDistinctSupplierCodes() {
		return baseMapper.selectDistinctSupplierCodes();
	}

	/**
	 * 获取所有销售国家列表（去重）
	 *
	 * @return 销售国家列表
	 */
	public List<String> getDistinctSalesCountries() {
		return baseMapper.selectDistinctSalesCountries();
	}

	/**
	 * 获取SKU数据概览统计
	 *
	 * @return 统计数据
	 */
	public Map<String, Object> getSkuOverviewStats() {
		return baseMapper.getSkuOverviewStats();
	}

	/**
	 * 获取筛选选项数据（用于前端筛选组件）
	 *
	 * @return 筛选选项
	 */
	public Map<String, Object> getFilterOptions() {
		Map<String, Object> options = new java.util.HashMap<>();

		// 品牌选项
		options.put("brands", getDistinctBrands());

		// 项目组选项
		options.put("projectGroups", getDistinctProjectGroups());

		// 供应商选项
		options.put("supplierCodes", getDistinctSupplierCodes());

		// 销售国家选项
		options.put("salesCountries", getDistinctSalesCountries());

		// 产品状态选项
		List<Map<String, Object>> productStatusOptions = new ArrayList<>();
		productStatusOptions.add(createOption(1, "在售"));
		productStatusOptions.add(createOption(2, "停售"));
		productStatusOptions.add(createOption(3, "开发中"));
		productStatusOptions.add(createOption(4, "已下架"));
		options.put("productStatuses", productStatusOptions);

		// 头程类型选项
		List<Map<String, Object>> shippingTypeOptions = new ArrayList<>();
		shippingTypeOptions.add(createOption(1, "陆运"));
		shippingTypeOptions.add(createOption(2, "空运"));
		options.put("shippingTypes", shippingTypeOptions);

		// 是否含税选项
		List<Map<String, Object>> includeTaxOptions = new ArrayList<>();
		includeTaxOptions.add(createOption(0, "否"));
		includeTaxOptions.add(createOption(1, "是"));
		options.put("includeTaxOptions", includeTaxOptions);

		// 是否有排插选项
		List<Map<String, Object>> needsPowerOptions = new ArrayList<>();
		needsPowerOptions.add(createOption(0, "否"));
		needsPowerOptions.add(createOption(1, "是"));
		options.put("needsPowerOptions", needsPowerOptions);

		// 是否季节性产品选项
		List<Map<String, Object>> isSeasonalOptions = new ArrayList<>();
		isSeasonalOptions.add(createOption(0, "否"));
		isSeasonalOptions.add(createOption(1, "是"));
		options.put("isSeasonalOptions", isSeasonalOptions);

		return options;
	}

	/**
	 * 创建选项对象
	 *
	 * @param value 值
	 * @param label 标签
	 * @return 选项对象
	 */
	private Map<String, Object> createOption(Object value, String label) {
		Map<String, Object> option = new java.util.HashMap<>();
		option.put("value", value);
		option.put("label", label);
		return option;
	}

	/**
	 * 批量操作：批量更新产品状态
	 *
	 * @param skuIds        SKU ID列表
	 * @param productStatus 新的产品状态
	 * @return 更新成功的数量
	 */
	public int batchUpdateProductStatus(List<Long> skuIds, Integer productStatus) {
		if (skuIds == null || skuIds.isEmpty()) {
			return 0;
		}

		int successCount = 0;
		for (Long skuId : skuIds) {
			Sku sku = this.getById(skuId);
			if (sku != null) {
				sku.setProductStatus(productStatus);
				if (this.updateById(sku)) {
					successCount++;
				}
			}
		}

		log.info("批量更新产品状态完成: 总数={}, 成功={}, 状态={}", skuIds.size(), successCount, productStatus);
		return successCount;
	}

	/**
	 * 导出查询结果
	 *
	 * @param qo 查询条件
	 * @return SKU列表
	 */
	public List<Sku> exportSkus(SkuQO qo) {
		// 限制导出数量，防止内存溢出
		List<Sku> result = baseMapper.selectByAdvancedSearch(qo);
		if (result.size() > 10000) {
			log.warn("导出数据量过大: {}, 已限制为前10000条", result.size());
			return result.subList(0, 10000);
		}
		return result;
	}

	/**
	 * 填充关联字段
	 *
	 * @param records SKU分页数据列表
	 */
	private void fillRelatedFields(List<SkuPageVO> records) {
		if (records == null || records.isEmpty()) {
			return;
		}

		// 收集需要查询的ID
		Set<Long> categoryIds = records.stream()
				.map(SkuPageVO::getCategoryId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		Set<String> brandCodes = records.stream()
				.map(SkuPageVO::getBrandCode)
				.filter(StringUtils::hasText)
				.collect(Collectors.toSet());

		Set<String> projectGroupCodes = records.stream()
				.map(SkuPageVO::getProjectGroupCode)
				.filter(StringUtils::hasText)
				.collect(Collectors.toSet());

		Set<Long> userIds = records.stream()
				.flatMap(record -> java.util.stream.Stream.of(record.getDeveloperId(), record.getOperatorId(),
						record.getQcId(), record.getPurchaserId()))
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		// 批量查询关联数据
		Map<Long, String> categoryMap = categoryIds.isEmpty() ? Collections.emptyMap()
				: this.categoryService.listByIds(categoryIds)
				.stream()
				.collect(Collectors.toMap(Category::getId, Category::getName));

		Map<String, String> brandMap = brandCodes.isEmpty() ? Collections.emptyMap()
				: this.brandService.list()
				.stream()
				.filter(brand -> brandCodes.contains(brand.getCode()))
				.collect(Collectors.toMap(Brand::getCode, Brand::getName));

		Map<String, String> projectGroupMap = projectGroupCodes.isEmpty() ? Collections.emptyMap()
				: this.projectGroupService.list()
				.stream()
				.filter(group -> projectGroupCodes.contains(group.getCode()))
				.collect(Collectors.toMap(ProjectGroup::getCode, ProjectGroup::getName));

		// 查询用户信息 - 这里需要根据实际的用户服务接口调整
		Map<Long, String> userMap = getUserNameMap(userIds);

		// 填充数据
		records.forEach(record -> {
			// 填充品类名称和层级信息
			if (record.getCategoryId() != null) {
				record.setCategoryName(categoryMap.get(record.getCategoryId()));
				// 填充品类层级信息
				record.setCategoryHierarchy(this.categoryService.buildCategoryHierarchy(record.getCategoryId()));
			}

			// 填充品牌名称
			if (StringUtils.hasText(record.getBrandCode())) {
				record.setBrandName(brandMap.get(record.getBrandCode()));
			}

			// 填充项目组名称
			if (StringUtils.hasText(record.getProjectGroupCode())) {
				record.setProjectGroupName(projectGroupMap.get(record.getProjectGroupCode()));
			}

			// 填充人员姓名
			if (record.getDeveloperId() != null) {
				record.setDeveloperName(userMap.get(record.getDeveloperId()));
			}
			if (record.getOperatorId() != null) {
				record.setOperatorName(userMap.get(record.getOperatorId()));
			}
			if (record.getQcId() != null) {
				record.setQcName(userMap.get(record.getQcId()));
			}
			if (record.getPurchaserId() != null) {
				record.setPurchaserName(userMap.get(record.getPurchaserId()));
			}
		});

		// 填充文件信息
		fillSkuFiles(records);

		// 填充映射数量
		fillMappingCounts(records);
	}

	/**
	 * 填充SKU映射数量
	 *
	 * @param records SKU分页数据列表
	 */
	private void fillMappingCounts(List<SkuPageVO> records) {
		if (records == null || records.isEmpty()) {
			return;
		}

		// 收集所有 SKU 编码
		List<String> skuCodes = records.stream()
				.map(SkuPageVO::getSkuCode)
				.filter(StringUtils::hasText)
				.collect(Collectors.toList());

		if (skuCodes.isEmpty()) {
			return;
		}

		// 批量查询映射数量，返回 DTO 列表
		List<SkuMappingCountDTO> countList = skuMappingMapper.countBySkuCodes(skuCodes);

		// 转换为 Map，方便查找
		Map<String, Integer> countMap = new HashMap<>();
		if (!CollectionUtils.isEmpty(countList)) {
			countMap = countList.stream()
					.collect(Collectors.toMap(SkuMappingCountDTO::getSkuCode, SkuMappingCountDTO::getCount));
		}

		// 填充到每个记录（使用 final 变量以便在 lambda 中使用）
		final Map<String, Integer> finalCountMap = countMap;
		records.forEach(record -> {
			Integer count = finalCountMap.getOrDefault(record.getSkuCode(), 0);
			record.setMappingCount(count);
		});
	}

	/**
	 * 为SKU分页数据填充文件信息
	 */
	private void fillSkuFiles(List<SkuPageVO> records) {
		if (CollectionUtils.isEmpty(records)) {
			return;
		}

		// 收集所有SKU ID
		List<Long> skuIds = records.stream().map(SkuPageVO::getId).collect(Collectors.toList());

		// 批量查询文件信息并按SKU ID分组
		Map<Long, Map<String, List<SkuFileVO>>> skuFilesMap = new HashMap<>();
		for (Long skuId : skuIds) {
			List<SkuFiles> files = this.skuFilesMapper.selectBySkuId(skuId);
			Map<String, List<SkuFileVO>> filesByType = files.stream()
					.map(this::convertToFileVO)
					.collect(Collectors.groupingBy(SkuFileVO::getFileType,
							Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
									.sorted(Comparator.comparing(vo -> vo.getSortOrder() != null ? vo.getSortOrder() : 0))
									.collect(Collectors.toList()))));
			skuFilesMap.put(skuId, filesByType);
		}

		// 填充文件信息到每个SKU
		records.forEach(record -> {
			Map<String, List<SkuFileVO>> files = skuFilesMap.getOrDefault(record.getId(), Collections.emptyMap());
			record.setFiles(files);
		});
	}

	/**
	 * 获取用户姓名映射
	 *
	 * @param userIds 用户ID集合
	 * @return 用户ID到姓名的映射
	 */
	private Map<Long, String> getUserNameMap(Set<Long> userIds) {
		if (userIds.isEmpty()) {
			return new HashMap<>();
		}
		try {
			List<SysUser> sysUsers = this.sysUserService.listByUserIds(userIds);
			if (CollectionUtils.isEmpty(sysUsers)) {
				return new HashMap<>();
			}

			return sysUsers.stream().collect(Collectors.toMap(SysUser::getUserId, SysUser::getNickname));
		} catch (Exception e) {
			log.error("Failed to get user name mapping", e);
			return new HashMap<>();
		}
	}

	// ==================== 文件相关方法 ====================

	/**
	 * 智能更新SKU文件映射（对比后增删改）
	 */
	private void updateSkuFilesFromMap(Long skuId, Map<String, List<SkuFileDTO>> newFilesMap) {
		// 1. 查询当前存在的文件
		List<SkuFiles> existingFiles = this.skuFilesMapper.selectBySkuId(skuId);

		// 2. 如果新文件映射为空，删除所有现有文件
		if (CollectionUtils.isEmpty(newFilesMap)) {
			if (!existingFiles.isEmpty()) {
				this.skuFilesMapper.deleteBySkuIdAndFileType(skuId, null);
				log.info("删除SKU所有文件: skuId={}, 删除数量={}", skuId, existingFiles.size());
			}
			return;
		}

		// 3. 将Map转换为List以便处理，并根据数组索引设置排序顺序
		List<SkuFileDTO> newFilesList = new ArrayList<>();
		for (Map.Entry<String, List<SkuFileDTO>> entry : newFilesMap.entrySet()) {
			String fileType = entry.getKey();
			List<SkuFileDTO> files = entry.getValue();
			if (!CollectionUtils.isEmpty(files)) {
				// 确保每个文件的fileType与map的key一致
				files.forEach(file -> file.setFileType(fileType));
				newFilesList.addAll(files);
			}
		}

		// 4. 创建对比用的映射关系
		// 使用文件类型+对象键作为唯一标识
		Map<String, SkuFiles> existingFileMap = existingFiles.stream()
				.collect(Collectors.toMap(file -> file.getFileType() + ":" + file.getObjectKey(), file -> file));

		// 为新文件创建带排序信息的映射
		Map<String, Integer> newFileSortOrderMap = new HashMap<>();
		for (Map.Entry<String, List<SkuFileDTO>> entry : newFilesMap.entrySet()) {
			String fileType = entry.getKey();
			List<SkuFileDTO> files = entry.getValue();
			if (!CollectionUtils.isEmpty(files)) {
				for (int i = 0; i < files.size(); i++) {
					SkuFileDTO file = files.get(i);
					String key = fileType + ":" + file.getObjectKey();
					newFileSortOrderMap.put(key, i);
				}
			}
		}

		Set<String> newFileKeys = newFileSortOrderMap.keySet();

		// 5. 找出需要删除的文件（存在于现有文件中，但不在新文件列表中）
		List<SkuFiles> filesToDelete = existingFiles.stream().filter(existingFile -> {
			String key = existingFile.getFileType() + ":" + existingFile.getObjectKey();
			return !newFileKeys.contains(key);
		}).collect(Collectors.toList());

		// 6. 找出需要新增的文件（存在于新文件列表中，但不在现有文件中）
		List<SkuFileDTO> filesToAdd = newFilesList.stream().filter(newFile -> {
			String key = newFile.getFileType() + ":" + newFile.getObjectKey();
			return !existingFileMap.containsKey(key);
		}).collect(Collectors.toList());

		// 7. 找出需要更新的文件（存在于两个列表中，但排序顺序可能不同）
		List<SkuFiles> filesToUpdate = new ArrayList<>();
		for (SkuFiles existingFile : existingFiles) {
			String key = existingFile.getFileType() + ":" + existingFile.getObjectKey();
			Integer newSortOrder = newFileSortOrderMap.get(key);
			if (newSortOrder != null) {
				// 检查排序顺序是否需要更新
				Integer existingSortOrder = existingFile.getSortOrder() != null ? existingFile.getSortOrder() : 0;

				if (!existingSortOrder.equals(newSortOrder)) {
					existingFile.setSortOrder(newSortOrder);
					filesToUpdate.add(existingFile);
				}
			}
		}

		// 8. 执行删除操作
		if (!filesToDelete.isEmpty()) {
			for (SkuFiles fileToDelete : filesToDelete) {
				this.skuFilesMapper.deleteById(fileToDelete.getId());
			}
			log.info("删除SKU文件: skuId={}, 删除数量={}", skuId, filesToDelete.size());
		}

		// 9. 执行更新操作
		if (!filesToUpdate.isEmpty()) {
			for (SkuFiles fileToUpdate : filesToUpdate) {
				this.skuFilesMapper.updateById(fileToUpdate);
			}
			log.info("更新SKU文件排序: skuId={}, 更新数量={}", skuId, filesToUpdate.size());
		}

		// 10. 执行新增操作
		if (!filesToAdd.isEmpty()) {
			saveSkuFilesWithOrder(skuId, filesToAdd, newFileSortOrderMap);
			log.info("新增SKU文件: skuId={}, 新增数量={}", skuId, filesToAdd.size());
		}

		log.info("SKU文件更新完成: skuId={}, 删除={}, 更新={}, 新增={}", skuId, filesToDelete.size(), filesToUpdate.size(),
				filesToAdd.size());
	}

	/**
	 * 从Map结构保存SKU文件列表
	 */
	private void saveSkuFilesFromMap(Long skuId, Map<String, List<SkuFileDTO>> filesMap) {
		if (CollectionUtils.isEmpty(filesMap)) {
			return;
		}

		List<SkuFiles> allSkuFiles = new ArrayList<>();
		for (Map.Entry<String, List<SkuFileDTO>> entry : filesMap.entrySet()) {
			String fileType = entry.getKey();
			List<SkuFileDTO> files = entry.getValue();
			if (!CollectionUtils.isEmpty(files)) {
				// 根据数组索引设置排序顺序
				for (int i = 0; i < files.size(); i++) {
					SkuFileDTO fileDTO = files.get(i);
					SkuFiles skuFiles = new SkuFiles();
					skuFiles.setSkuId(skuId);
					skuFiles.setFileType(fileType);
					skuFiles.setObjectKey(fileDTO.getObjectKey());
					skuFiles.setSortOrder(i); // 使用数组索引作为排序顺序
					allSkuFiles.add(skuFiles);
				}
			}
		}

		if (!allSkuFiles.isEmpty()) {
			allSkuFiles.forEach(this.skuFilesMapper::insert);
		}
	}

	/**
	 * 保存SKU文件列表（带排序信息）
	 */
	private void saveSkuFilesWithOrder(Long skuId, List<SkuFileDTO> files, Map<String, Integer> sortOrderMap) {
		if (CollectionUtils.isEmpty(files)) {
			return;
		}

		List<SkuFiles> skuFilesList = files.stream().map(fileDTO -> {
			SkuFiles skuFiles = new SkuFiles();
			skuFiles.setSkuId(skuId);
			skuFiles.setFileType(fileDTO.getFileType());
			skuFiles.setObjectKey(fileDTO.getObjectKey());

			// 根据文件类型和对象键获取排序顺序
			String key = fileDTO.getFileType() + ":" + fileDTO.getObjectKey();
			Integer sortOrder = sortOrderMap.get(key);
			skuFiles.setSortOrder(sortOrder != null ? sortOrder : 0);

			return skuFiles;
		}).collect(Collectors.toList());

		skuFilesList.forEach(this.skuFilesMapper::insert);
	}

	/**
	 * 获取SKU文件映射
	 *
	 * @param skuId SKU ID
	 * @return 文件映射，key为文件类型，value为文件列表（按sortOrder排序）
	 */
	public Map<String, List<SkuFileVO>> getSkuFilesMap(Long skuId) {
		List<SkuFiles> files = this.skuFilesMapper.selectBySkuId(skuId);
		return files.stream()
				.map(this::convertToFileVO)
				.collect(Collectors.groupingBy(SkuFileVO::getFileType,
						Collectors.collectingAndThen(Collectors.toList(),
								list -> list.stream()
										.sorted(Comparator.comparing(vo -> vo.getSortOrder() != null ? vo.getSortOrder() : 0))
										.collect(Collectors.toList()))));
	}

	/**
	 * 获取SKU文件列表
	 *
	 * @param skuId SKU ID
	 * @return 文件列表（按sortOrder排序）
	 */
	public List<SkuFileVO> getSkuFiles(Long skuId) {
		List<SkuFiles> files = this.skuFilesMapper.selectBySkuId(skuId);
		return files.stream()
				.map(this::convertToFileVO)
				.sorted(Comparator.comparing(vo -> vo.getSortOrder() != null ? vo.getSortOrder() : 0))
				.collect(Collectors.toList());
	}

	/**
	 * 转换文件实体为VO
	 */
	private SkuFileVO convertToFileVO(SkuFiles skuFiles) {
		SkuFileVO vo = new SkuFileVO();
		vo.setId(skuFiles.getId());
		vo.setFileType(skuFiles.getFileType());
		vo.setObjectKey(skuFiles.getObjectKey());
		vo.setSortOrder(skuFiles.getSortOrder());
		vo.setFileUrl(ossService.getUrl(OssBucketKeys.PUBLIC_FILES, skuFiles.getObjectKey()));
		return vo;
	}

	/**
	 * 删除SKU文件
	 *
	 * @param skuId    SKU ID
	 * @param fileType 文件类型（可选，为空时删除所有文件）
	 * @return 是否成功
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteSkuFiles(Long skuId, String fileType) {
		int deleted = this.skuFilesMapper.deleteBySkuIdAndFileType(skuId, fileType);
		return deleted > 0;
	}

	/**
	 * 获取SKU详情（包含文件信息）
	 *
	 * @param skuId SKU ID
	 * @return SKU详情VO
	 */
	public SkuDetailVO getSkuDetailWithFiles(Long skuId) {
		Sku sku = this.getById(skuId);
		if (sku == null) {
			return null;
		}

		// 转换为详情VO（这里需要实现转换逻辑，或者使用converter）
		SkuDetailVO detailVO = convertToDetailVO(sku);

		// 添加文件信息（使用Map结构）
		Map<String, List<SkuFileVO>> filesMap = getSkuFilesMap(skuId);
		detailVO.setFiles(filesMap);

		return detailVO;
	}

	/**
	 * 转换SKU实体为详情VO
	 */
	private SkuDetailVO convertToDetailVO(Sku sku) {
		// 使用MapStruct转换器进行基础属性映射
		SkuDetailVO detailVO = SkuConverter.INSTANCE.poToDetailVo(sku);

		// 填充关联信息名称
		fillDetailVONames(detailVO, sku);

		return detailVO;
	}

	/**
	 * 填充详情VO的关联名称
	 */
	private void fillDetailVONames(SkuDetailVO detailVO, Sku sku) {
		// 填充品类名称
		if (sku.getCategoryId() != null) {
			Category category = this.categoryService.getById(sku.getCategoryId());
			if (category != null) {
				detailVO.setCategoryName(category.getName());
			}
		}

		// 填充品牌名称
		if (StringUtils.hasText(sku.getBrandCode())) {
			List<Brand> brands = this.brandService.list();
			brands.stream()
					.filter(b -> Objects.equals(b.getCode(), sku.getBrandCode()))
					.findFirst()
					.ifPresent(brand -> detailVO.setBrandName(brand.getName()));
		}

		// 填充项目组名称
		if (StringUtils.hasText(sku.getProjectGroupCode())) {
			// 假设需要从所有项目组中查找匹配的代码
			List<ProjectGroup> projectGroups = this.projectGroupService.list();
			projectGroups.stream()
					.filter(pg -> Objects.equals(pg.getCode(), sku.getProjectGroupCode()))
					.findFirst()
					.ifPresent(projectGroup -> detailVO.setProjectGroupName(projectGroup.getName()));
		}

		// 填充人员名称
		Set<Long> userIds = new HashSet<>();
		if (sku.getDeveloperId() != null) {
			userIds.add(sku.getDeveloperId());
		}
		if (sku.getOperatorId() != null) {
			userIds.add(sku.getOperatorId());
		}
		if (sku.getQcId() != null) {
			userIds.add(sku.getQcId());
		}
		if (sku.getPurchaserId() != null) {
			userIds.add(sku.getPurchaserId());
		}

		if (!userIds.isEmpty()) {
			Map<Long, String> userMap = getUserNameMap(userIds);
			if (sku.getDeveloperId() != null) {
				detailVO.setDeveloperName(userMap.get(sku.getDeveloperId()));
			}
			if (sku.getOperatorId() != null) {
				detailVO.setOperatorName(userMap.get(sku.getOperatorId()));
			}
			if (sku.getQcId() != null) {
				detailVO.setQcName(userMap.get(sku.getQcId()));
			}
			if (sku.getPurchaserId() != null) {
				detailVO.setPurchaserName(userMap.get(sku.getPurchaserId()));
			}
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long id) {
		Sku sku = this.baseMapper.selectById(id);
		if (sku == null) {
			return false;
		}

		int i = this.baseMapper.deleteById(id);
		Assert.isTrue(i > 0, "删除失败");

		// 删除SKU映射
		this.skuMappingMapper.deleteBySkuCode(sku.getSkuCode());
		return true;
	}

	// ==================== SKU选择弹窗专用查询 ====================

	/**
	 * SKU选择弹窗分页查询（轻量级）
	 * @param pageParam 分页参数
	 * @param qo 查询参数
	 * @return PageResult<SkuSelectVO> 分页数据
	 */
	public PageResult<SkuSelectVO> querySelectPage(PageParam pageParam, SkuSelectQO qo) {
		// 平台端跨货主选品：按所选货主 tenant_id 临时切上下文，查其 SKU 目录（含图片/品类富化）
		Long ownerScope = platformOwnerScope(qo.getErpTenantId());
		if (ownerScope != null) {
			return TenantContext.runAs(ownerScope, () -> doQuerySelectPage(pageParam, qo));
		}
		return doQuerySelectPage(pageParam, qo);
	}

	private PageResult<SkuSelectVO> doQuerySelectPage(PageParam pageParam, SkuSelectQO qo) {
		PageResult<Sku> pageResult = baseMapper.querySelectPage(pageParam, qo);

		// 转换为 SkuSelectVO 并填充主图和品类路径
		List<SkuSelectVO> voList = convertToSkuSelectVOList(pageResult.getRecords());
		return new PageResult<>(voList, pageResult.getTotal());
	}

	/**
	 * 平台身份且指定货主时返回该货主 tenant_id（用于跨租户查其 SKU 目录）；否则返回 null（走当前租户，防越权）。
	 * @param erpTenantId 请求指定的货主ID
	 * @return 需临时切换的货主 tenant_id，或 null
	 */
	private Long platformOwnerScope(Long erpTenantId) {
		if (erpTenantId == null) {
			return null;
		}
		String type = tenantIdentityService.currentIdentity(null).getIdentityType();
		return TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(type) ? erpTenantId : null;
	}

	/**
	 * 根据SKU编码批量查询（用于选择弹窗回显）
	 * @param skuCodes SKU编码列表，最多100条
	 * @return List<SkuSelectVO> SKU列表
	 */
	public List<SkuSelectVO> listByCodes(List<String> skuCodes) {
		if (CollectionUtils.isEmpty(skuCodes)) {
			return Collections.emptyList();
		}

		// 校验最大数量限制
		if (skuCodes.size() > 100) {
			throw new IllegalArgumentException("SKU编码数量不能超过100条");
		}

		List<Sku> skuList = baseMapper.selectBySkuCodes(skuCodes);
		return convertToSkuSelectVOList(skuList);
	}

	/**
	 * 将SKU实体列表转换为SkuSelectVO列表，并填充主图和品类路径
	 */
	private List<SkuSelectVO> convertToSkuSelectVOList(List<Sku> skuList) {
		if (CollectionUtils.isEmpty(skuList)) {
			return Collections.emptyList();
		}

		// 收集需要查询的ID
		List<Long> skuIds = skuList.stream().map(Sku::getId).distinct().collect(Collectors.toList());

		// 批量查询主图
		Map<Long, String> mainImageMap = skuImageResolver.resolveForQuery(skuIds);

		// 转换
		return skuList.stream().map(sku -> {
			SkuSelectVO vo = new SkuSelectVO();
			vo.setId(sku.getId());
			vo.setSkuCode(sku.getSkuCode());
			vo.setChineseName(sku.getChineseName());
			vo.setMainImage(mainImageMap.get(sku.getId()));
			CategoryHierarchyVO categoryHierarchyVO = this.categoryService.buildCategoryHierarchy(sku.getCategoryId());
			if(categoryHierarchyVO != null) {
				vo.setCategoryFullPath(categoryHierarchyVO.getFullPathName());
			}
			return vo;
		}).collect(Collectors.toList());
	}

}
