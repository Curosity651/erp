package com.erp.admin.product.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.erp.admin.product.enums.ImageScene;
import com.erp.admin.product.mapper.SkuFilesMapper;
import com.erp.admin.product.model.entity.SkuFiles;
import com.erp.admin.system.config.AliyunOssProperties;
import com.erp.admin.system.config.OssBucketKeys;
import com.erp.admin.system.config.OssImageStyles;
import com.erp.admin.system.model.dto.OssUrlOptions;
import com.erp.admin.system.service.OssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.toolkit.WrappersX;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * SKU 图片解析器
 * <p>
 * 统一处理 SKU 图片的查询、优先级选择和 URL 构建逻辑
 * </p>
 *
 * @author system
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkuImageResolver {

	private final SkuFilesMapper skuFilesMapper;

	private final OssService ossService;

	private final AliyunOssProperties ossProperties;

	/**
	 * 默认图片类型优先级（用于 SKU 导出和订单导出）
	 */
	public static final List<String> DEFAULT_IMAGE_TYPES = Arrays.asList("platform_image", "actual_image",
			"product_image");

	/**
	 * 批量查询的最大批次大小,避免 SQL IN 子句过大导致性能问题
	 */
	private static final int MAX_BATCH_SIZE = 500;

	// ==================== 场景化 API ====================

	/**
	 * 查询场景解析（外网 + 缩略图）
	 *
	 * @param skuIds SKU ID 列表
	 * @return Map<skuId, imageUrl>
	 */
	public Map<Long, String> resolveForQuery(List<Long> skuIds) {
		return resolveForQuery(skuIds, DEFAULT_IMAGE_TYPES);
	}

	/**
	 * 查询场景解析（外网 + 缩略图，自定义图片类型）
	 *
	 * @param skuIds     SKU ID 列表
	 * @param imageTypes 图片类型优先级列表
	 * @return Map<skuId, imageUrl>
	 */
	public Map<Long, String> resolveForQuery(List<Long> skuIds, List<String> imageTypes) {
		return doResolveByScene(skuIds, imageTypes, ImageScene.QUERY);
	}

	/**
	 * 导出场景解析（根据配置决定内网/外网 + 缩略图）
	 *
	 * @param skuIds SKU ID 列表
	 * @return Map<skuId, imageUrl>
	 */
	public Map<Long, String> resolveForExport(List<Long> skuIds) {
		return resolveForExport(skuIds, DEFAULT_IMAGE_TYPES);
	}

	/**
	 * 导出场景解析（根据配置决定内网/外网 + 缩略图，自定义图片类型）
	 *
	 * @param skuIds     SKU ID 列表
	 * @param imageTypes 图片类型优先级列表
	 * @return Map<skuId, imageUrl>
	 */
	public Map<Long, String> resolveForExport(List<Long> skuIds, List<String> imageTypes) {
		return doResolveByScene(skuIds, imageTypes, ImageScene.EXPORT);
	}

	/**
	 * 查询场景解析（返回 URL 对象，外网 + 缩略图）
	 *
	 * @param skuIds SKU ID 列表
	 * @return Map<skuId, URL>
	 */
	public Map<Long, URL> resolveForQueryAsURL(List<Long> skuIds) {
		return convertToURL(resolveForQuery(skuIds));
	}

	/**
	 * 导出场景解析（返回 URL 对象，根据配置决定内网/外网 + 缩略图）
	 *
	 * @param skuIds SKU ID 列表
	 * @return Map<skuId, URL>
	 */
	public Map<Long, URL> resolveForExportAsURL(List<Long> skuIds) {
		return convertToURL(resolveForExport(skuIds));
	}

	/**
	 * 将 String URL Map 转换为 URL 对象 Map
	 *
	 * @param urlStringMap String URL 映射
	 * @return URL 对象映射
	 */
	private Map<Long, URL> convertToURL(Map<Long, String> urlStringMap) {
		if (CollectionUtils.isEmpty(urlStringMap)) {
			return Collections.emptyMap();
		}

		Map<Long, URL> resultMap = new HashMap<>(calculateInitialCapacity(urlStringMap.size()));
		for (Map.Entry<Long, String> entry : urlStringMap.entrySet()) {
			try {
				resultMap.put(entry.getKey(), new URL(entry.getValue()));
			} catch (MalformedURLException e) {
				log.warn("Invalid URL format for skuId={}: {}", entry.getKey(), entry.getValue(), e);
			}
		}
		return resultMap;
	}

	/**
	 * 统一解析方法（内部使用）
	 *
	 * @param skuIds     SKU ID 列表
	 * @param imageTypes 图片类型优先级列表
	 * @param scene      图片场景
	 * @return Map<skuId, imageUrl>
	 */
	private Map<Long, String> doResolveByScene(List<Long> skuIds, List<String> imageTypes, ImageScene scene) {
		if (CollectionUtils.isEmpty(skuIds) || CollectionUtils.isEmpty(imageTypes)) {
			return Collections.emptyMap();
		}

		// 根据场景确定参数
		boolean useInternal;
		String style;

		if (scene == ImageScene.EXPORT) {
			useInternal = ossProperties.getExport() != null
					&& Boolean.TRUE.equals(ossProperties.getExport().getUseInternalDomain());
			style = ossProperties.getExport() != null
					? ossProperties.getExport().getImageStyle()
					: OssImageStyles.EXPORT_THUMBNAIL;
		} else {
			// QUERY 场景：外网 + 缩略图
			useInternal = false;
			style = OssImageStyles.EXPORT_THUMBNAIL;
		}

		// 如果 skuIds 数量超过批次大小,分批查询
		if (skuIds.size() > MAX_BATCH_SIZE) {
			return batchResolveInChunks(skuIds, imageTypes, useInternal, style);
		}

		return doQueryAndResolve(skuIds, imageTypes, useInternal, style);
	}

	// ==================== 原有方法（保留兼容） ====================

	/**
	 * HashMap 负载因子,用于计算初始容量避免扩容
	 */
	private static final float LOAD_FACTOR = 0.75f;

	/**
	 * 文件排序比较器:按排序号升序,创建时间升序
	 */
	private static final Comparator<SkuFiles> FILE_COMPARATOR = Comparator
			.comparing(SkuFiles::getSortOrder, Comparator.nullsLast(Integer::compareTo))
			.thenComparing(SkuFiles::getCreateTime, Comparator.nullsLast(LocalDateTime::compareTo));

	/**
	 * 分批查询并合并结果
	 */
	private Map<Long, String> batchResolveInChunks(List<Long> skuIds, List<String> imageTypes,
												   boolean useInternal, String style) {
		Map<Long, String> result = new HashMap<>(calculateInitialCapacity(skuIds.size()));

		for (int i = 0; i < skuIds.size(); i += MAX_BATCH_SIZE) {
			int endIndex = Math.min(i + MAX_BATCH_SIZE, skuIds.size());
			List<Long> batch = skuIds.subList(i, endIndex);
			result.putAll(doQueryAndResolve(batch, imageTypes, useInternal, style));
		}

		return result;
	}

	/**
	 * 执行查询并解析图片 URL
	 */
	private Map<Long, String> doQueryAndResolve(List<Long> skuIds, List<String> imageTypes,
												boolean useInternal, String style) {
		// 1. 批量查询所有相关的文件记录
		List<SkuFiles> allFiles = skuFilesMapper.selectBySkuIdsAndFileTypes(skuIds, imageTypes);

		if (CollectionUtils.isEmpty(allFiles)) {
			return Collections.emptyMap();
		}

		// 2. 一次循环完成：为每个 SKU 选择最佳候选文件
		Map<Long, SkuFiles> bestFilePerSku = new HashMap<>(calculateInitialCapacity(skuIds.size()));

		for (SkuFiles file : allFiles) {
			// 跳过 objectKey 为空的文件
			if (!StringUtils.hasText(file.getObjectKey())) {
				continue;
			}

			Long skuId = file.getSkuId();
			SkuFiles currentBest = bestFilePerSku.get(skuId);

			// 判断当前文件是否比已有的"最佳候选"更优
			if (isBetterCandidate(file, currentBest, imageTypes)) {
				bestFilePerSku.put(skuId, file);
			}
		}

		// 3. 构建图片 URL
		OssUrlOptions options = buildUrlOptions(useInternal, style);
		Map<Long, String> resultMap = new HashMap<>(calculateInitialCapacity(bestFilePerSku.size()));

		for (Map.Entry<Long, SkuFiles> entry : bestFilePerSku.entrySet()) {
				String url = ossService.getUrl(OssBucketKeys.PUBLIC_FILES, entry.getValue().getObjectKey(), options);
			if (url != null) {
				resultMap.put(entry.getKey(), url);
			}
		}

		return resultMap;
	}

	/**
	 * 构建 URL 选项
	 */
	private OssUrlOptions buildUrlOptions(boolean useInternal, String style) {
		if (useInternal) {
			String scheme = ossProperties.getExport() != null
					? ossProperties.getExport().getImageScheme() : "http";
			return OssUrlOptions.internalWithStyle(style, scheme);
		}
		if (StringUtils.hasText(style)) {
			return OssUrlOptions.withStyle(style);
		}
		return OssUrlOptions.none();
	}

	/**
	 * 判断新文件是否比当前最佳候选更优
	 *
	 * @param newFile     新文件
	 * @param currentBest 当前最佳候选文件
	 * @param imageTypes  图片类型优先级列表
	 * @return true 表示新文件更优
	 */
	private boolean isBetterCandidate(SkuFiles newFile, SkuFiles currentBest, List<String> imageTypes) {
		// 如果没有当前最佳候选，新文件直接成为最佳候选
		if (currentBest == null) {
			return true;
		}

		int newPriority = imageTypes.indexOf(newFile.getFileType());
		int currentPriority = imageTypes.indexOf(currentBest.getFileType());

		// 1. 优先比较图片类型优先级（索引越小优先级越高）
		if (newPriority != currentPriority) {
			// 如果新文件类型在优先级列表中，且当前最佳不在或新文件优先级更高
			return newPriority >= 0 && (currentPriority < 0 || newPriority < currentPriority);
		}

		// 2. 类型相同时，比较排序号和创建时间
		return FILE_COMPARATOR.compare(newFile, currentBest) < 0;
	}

	/**
	 * 计算 HashMap 的初始容量,避免扩容
	 *
	 * @param expectedSize 预期大小
	 * @return 初始容量
	 */
	private int calculateInitialCapacity(int expectedSize) {
		return (int) Math.ceil(expectedSize / LOAD_FACTOR);
	}

}
