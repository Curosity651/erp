package com.erp.admin.product.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.admin.product.mapper.CategoryMapper;
import com.erp.admin.product.mapper.SkuMappingMapper;
import com.erp.admin.product.model.entity.Category;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.model.entity.SkuMapping;
import com.erp.admin.product.model.qo.SkuMappingQO;
import com.erp.admin.product.model.vo.SkuMappingPageVO;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.mybatisplus.toolkit.WrappersX;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * SKU映射服务
 * <p>
 * 职责：管理平台商品ID (platformItemId) 与 ERP SKU编码 (skuCode) 的映射关系
 * <p>
 * 术语说明：
 * - platformItemId: 平台商品标识（Ozon 的 offer_id，Wildberries 的 article）
 * - skuCode: ERP 内部 SKU 编码
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class SkuMappingService extends ExtendServiceImpl<SkuMappingMapper, SkuMapping> {

	private final SkuService skuService;

	private final CategoryMapper categoryMapper;


	// ==================== 分页查询 ====================

	/**
	 * 分页查询（仅返回基础数据，SKU 信息由 Controller 层填充）
	 *
	 * @param pageParam 分页参数
	 * @param qo        查询参数对象
	 * @return PageResult<SkuMappingPageVO> 分页数据
	 */
	public PageResult<SkuMappingPageVO> queryPage(PageParam pageParam, SkuMappingQO qo) {
		PageResult<SkuMappingPageVO> page = baseMapper.queryPage(pageParam, qo);
		List<SkuMappingPageVO> records = page.getRecords();
		if (records.isEmpty()) {
			return page;
		}
		// 收集 skCode
		Set<String> skuCodes = records.stream().map(SkuMappingPageVO::getSkuCode)
				.filter(StringUtils::hasText)
				.collect(Collectors.toSet());
		if(skuCodes.isEmpty()) {
			return page;
		}

		// 批量查询 SKU 信息
		List<Sku> skus = skuService.listBySkuCodes(skuCodes);
		if(CollectionUtils.isEmpty(skus)) {
			return page;
		}

		Map<String, Sku> skuMap = skus.stream().collect(Collectors.toMap(Sku::getSkuCode, s -> s));

		// categoryId -> name
		Set<Long> categoryIds = skus.stream()
				.map(Sku::getCategoryId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		Map<Long, String> categoryNameMap = new HashMap<>();
		if (!categoryIds.isEmpty()) {
			List<Category> categories = categoryMapper.selectBatchIds(categoryIds);
			for (Category c : categories) {
				categoryNameMap.put(c.getId(), c.getName());
			}
		}

		// 回填
		for (SkuMappingPageVO vo : records) {
			if (vo.getSkuCode() != null) {
				Sku sku = skuMap.get(vo.getSkuCode());
				if (sku != null) {
					vo.setSkuChineseName(sku.getChineseName());
					if (sku.getCategoryId() != null) {
						vo.setCategoryName(categoryNameMap.get(sku.getCategoryId()));
					}
				}
			}
		}
		return page;
	}

	/**
	 * 批量查询 platformItemId -> skuCode 映射
	 *
	 * @param platformItemIds 平台商品ID列表
	 * @return Map<platformItemId, skuCode>，未映射的不包含在结果中
	 */
	public Map<String, String> getSkuCodeMapByPlatformItemIds(Collection<String> platformItemIds) {
		List<SkuMapping> mappings = this.listByPlatformItemIds(platformItemIds);
		return mappings.stream()
				.filter(m -> StringUtils.hasText(m.getSkuCode()))
				.collect(Collectors.toMap(
						SkuMapping::getPlatformItemId,
						SkuMapping::getSkuCode,
						(old, now) -> old
				));
	}

	/**
	 * 批量查询 SkuMapping 列表
	 *
	 * @param platformItemIds 平台商品ID集合
	 * @return SKU 映射列表
	 */
	public List<SkuMapping> listByPlatformItemIds(Collection<String> platformItemIds) {
		if (CollectionUtils.isEmpty(platformItemIds)) {
			return Collections.emptyList();
		}
		return baseMapper.selectByPlatformItemIds(new ArrayList<>(platformItemIds));
	}

	// ==================== skuCode -> platformItemId 映射 ====================

	/**
	 * 根据 skuCode 查询对应的平台商品ID列表
	 *
	 * @param skuCode SKU 编码
	 * @return 平台商品ID列表，无映射时返回空列表
	 */
	public List<String> getPlatformItemIdsBySkuCode(String skuCode) {
		List<SkuMapping> mappings = this.listBySkuCode(skuCode);
		if (CollectionUtils.isEmpty(mappings)) {
			return Collections.emptyList();
		}

		return mappings.stream()
				.map(SkuMapping::getPlatformItemId)
				.filter(StringUtils::hasText)
				.collect(Collectors.toList());
	}

	/**
	 * 批量根据 skuCode 查询对应的平台商品ID列表
	 *
	 * @param skuCodes SKU 编码列表
	 * @return 平台商品ID列表，无映射时返回空列表
	 */
	public List<String> getPlatformItemIdsBySkuCodes(Collection<String> skuCodes) {
		if (CollectionUtils.isEmpty(skuCodes)) {
			return Collections.emptyList();
		}
		return baseMapper.selectPlatformItemIdsBySkuCodes(new ArrayList<>(skuCodes));
	}

	/**
	 * 将 skuCodes 解析为 platformItemIds，用于查询预处理。
	 * 无映射时返回包含占位值的列表，确保调用方 SQL 返回空结果而非全量。
	 */
	public List<String> resolvePlatformItemIdsForQuery(Collection<String> skuCodes) {
		if (CollectionUtils.isEmpty(skuCodes)) {
			return Collections.emptyList();
		}
		List<String> platformItemIds = getPlatformItemIdsBySkuCodes(new ArrayList<>(skuCodes));
		if (platformItemIds.isEmpty()) {
			return Collections.singletonList("__NO_MATCH__");
		}
		return platformItemIds;
	}

	/**
	 * 根据 skuCode 查询映射列表
	 *
	 * @param skuCode SKU编码
	 * @return 映射列表
	 */
	public List<SkuMapping> listBySkuCode(String skuCode) {
		if (!StringUtils.hasText(skuCode)) {
			return Collections.emptyList();
		}
		return baseMapper.selectBySkuCode(skuCode);
	}

	// ==================== 校验方法 ====================

	/**
	 * 根据平台商品ID获取SKU编码
	 * @param platformItemId 平台商品ID（如 article/offer_id）
	 * @return SKU编码，未找到返回 null
	 */
	public String getSkuCodeByPlatformItemId(String platformItemId) {
		if (!StringUtils.hasText(platformItemId)) {
			return null;
		}
		List<SkuMapping> mappings = baseMapper.selectByPlatformItemIds(Collections.singletonList(platformItemId));
		if (CollectionUtils.isEmpty(mappings)) {
			return null;
		}
		return mappings.get(0).getSkuCode();
	}

	/**
	 * 校验 platformItemId 是否唯一
	 *
	 * @param platformItemId 平台商品ID
	 * @param excludeId      更新场景下排除自身ID
	 * @return true 唯一 | false 已存在
	 */
	public boolean validateUnique(String platformItemId, Long excludeId) {
		if (platformItemId == null) {
			return false;
		}
		LambdaQueryWrapperX<SkuMapping> wrapper = WrappersX.lambdaQueryX(SkuMapping.class)
				.eq(SkuMapping::getPlatformItemId, platformItemId)
				.neIfPresent(SkuMapping::getId, excludeId)
				.last("limit 1");
		return baseMapper.selectCount(wrapper) == 0;
	}

	/**
	 * 验证 SKU 编码是否存在
	 *
	 * @param skuCode SKU编码
	 * @return true-存在，false-不存在
	 */
	public boolean validateSkuExists(String skuCode) {
		if (!StringUtils.hasText(skuCode)) {
			return false;
		}
		Sku sku = skuService.getBySkuCode(skuCode);
		return sku != null;
	}

	// ==================== 导出 ====================

	/**
	 * 导出列表（根据当前过滤条件）
	 */
	public List<SkuMapping> listForExport(SkuMappingQO qo) {
		LambdaQueryWrapperX<SkuMapping> wrapper = WrappersX.lambdaQueryX(SkuMapping.class)
				.eqIfPresent(SkuMapping::getPlatformItemId, qo.getPlatformItemId())
				.eqIfPresent(SkuMapping::getSkuCode, qo.getSkuCode())
				.orderByDesc(SkuMapping::getId);
		return baseMapper.selectList(wrapper);
	}

}