package com.erp.admin.product.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.erp.admin.product.model.vo.SkuBriefVO;
import com.erp.admin.product.enums.ImageScene;
import com.erp.admin.product.mapper.SkuMapper;
import com.erp.admin.product.mapper.SkuMappingMapper;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.model.entity.SkuMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * SKU 简要信息服务
 * <p>
 * 提供基于 skuCode 的 SKU 简要信息（编码、名称、主图）批量查询和填充能力。
 * <p>
 * 使用场景：
 * <ul>
 *     <li>订单列表/导出 - SKU 信息展示</li>
 *     <li>库存列表/导出 - SKU 信息展示</li>
 *     <li>流水记录 - SKU 信息展示</li>
 * </ul>
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkuBriefService {

    private final SkuMapper skuMapper;

    private final SkuMappingMapper skuMappingMapper;

    private final SkuImageResolver skuImageResolver;

    // ==================== 查询场景 API ====================

    /**
     * 查询场景填充（外网 + 缩略图）
     *
     * @param voList           VO 列表
     * @param skuCodeExtractor SKU 编码提取函数
     * @param briefSetter      SkuBriefVO 设置函数
     * @param <T>              VO 类型
     */
    public <T> void enrichForQuery(
            List<T> voList,
            Function<T, String> skuCodeExtractor,
            BiConsumer<T, SkuBriefVO> briefSetter) {
        doEnrich(voList, skuCodeExtractor, briefSetter, ImageScene.QUERY);
    }

    /**
     * 批量构建 SKU 简要信息映射（查询场景）
     *
     * @param skuCodes SKU 编码集合
     * @return Map<skuCode, SkuBriefVO>
     */
    public Map<String, SkuBriefVO> buildMapForQuery(Collection<String> skuCodes) {
        return doBuildMap(skuCodes, ImageScene.QUERY);
    }

    // ==================== 导出场景 API ====================

    /**
     * 导出场景填充（根据配置决定内网/外网 + 缩略图）
     *
     * @param voList           VO 列表
     * @param skuCodeExtractor SKU 编码提取函数
     * @param briefSetter      SkuBriefVO 设置函数
     * @param <T>              VO 类型
     */
    public <T> void enrichForExport(
            List<T> voList,
            Function<T, String> skuCodeExtractor,
            BiConsumer<T, SkuBriefVO> briefSetter) {
        doEnrich(voList, skuCodeExtractor, briefSetter, ImageScene.EXPORT);
    }

    /**
     * 批量构建 SKU 简要信息映射（导出场景）
     *
     * @param skuCodes SKU 编码集合
     * @return Map<skuCode, SkuBriefVO>
     */
    public Map<String, SkuBriefVO> buildMapForExport(Collection<String> skuCodes) {
        return doBuildMap(skuCodes, ImageScene.EXPORT);
    }

    // ==================== 基于 Article 的 API ====================

    /**
     * 基于 article (platformItemId) 填充 SKU 简要信息
     * <p>
     * 完成 article → skuCode → SkuBriefVO 的两步映射
     *
     * @param voList           VO 列表
     * @param articleExtractor article 提取函数
     * @param briefSetter      SkuBriefVO 设置函数
     * @param scene            图片场景（QUERY/EXPORT）
     * @param <T>              VO 类型
     */
    public <T> void enrichByArticle(
            List<T> voList,
            Function<T, String> articleExtractor,
            BiConsumer<T, SkuBriefVO> briefSetter,
            ImageScene scene) {
        if (CollectionUtils.isEmpty(voList)) {
            return;
        }

        // 1. 收集所有 article
        List<String> articles = voList.stream()
                .map(articleExtractor)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        if (articles.isEmpty()) {
            return;
        }

        // 2. 批量查询 article → skuCode 映射
        List<SkuMapping> mappings = skuMappingMapper.selectByPlatformItemIds(articles);
        Map<String, String> articleToSkuCode = mappings.stream()
                .filter(m -> m != null && StringUtils.hasText(m.getPlatformItemId()))
                .collect(Collectors.toMap(
                        SkuMapping::getPlatformItemId,
                        SkuMapping::getSkuCode,
                        (old, now) -> old
                ));

        // 3. 复用现有方法获取 SkuBriefVO
        Collection<String> skuCodes = articleToSkuCode.values();
        Map<String, SkuBriefVO> briefMap = doBuildMap(skuCodes, scene);

        // 4. 填充到 VO
        for (T vo : voList) {
            String article = articleExtractor.apply(vo);
            if (StringUtils.hasText(article)) {
                String skuCode = articleToSkuCode.get(article);
                if (skuCode != null) {
                    briefSetter.accept(vo, briefMap.get(skuCode));
                }
            }
        }
    }

    // ==================== 内部实现 ====================

    /**
     * 统一填充方法
     */
    private <T> void doEnrich(
            List<T> voList,
            Function<T, String> skuCodeExtractor,
            BiConsumer<T, SkuBriefVO> briefSetter,
            ImageScene scene) {
        if (CollectionUtils.isEmpty(voList)) {
            return;
        }

        // 收集所有 skuCode
        List<String> skuCodes = voList.stream()
                .map(skuCodeExtractor)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        if (skuCodes.isEmpty()) {
            return;
        }

        // 构建映射表
        Map<String, SkuBriefVO> briefMap = doBuildMap(skuCodes, scene);

        // 填充到 VO
        for (T vo : voList) {
            String skuCode = skuCodeExtractor.apply(vo);
            if (StringUtils.hasText(skuCode)) {
                SkuBriefVO brief = briefMap.get(skuCode);
                if (brief != null) {
                    briefSetter.accept(vo, brief);
                }
            }
        }
    }

    /**
     * 统一构建映射表方法
     */
    private Map<String, SkuBriefVO> doBuildMap(Collection<String> skuCodes, ImageScene scene) {
        if (CollectionUtils.isEmpty(skuCodes)) {
            return Collections.emptyMap();
        }

        // 过滤空值并去重
        List<String> validCodes = skuCodes.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        if (validCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        // 批量查询 SKU 信息
        List<Sku> skuList = skuMapper.selectBySkuCodes(validCodes);

        if (CollectionUtils.isEmpty(skuList)) {
            return Collections.emptyMap();
        }

        // 批量获取 SKU 图片
        List<Long> skuIds = skuList.stream()
                .map(Sku::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<Long, String> imageUrlMap = (scene == ImageScene.EXPORT)
                ? skuImageResolver.resolveForExport(skuIds)
                : skuImageResolver.resolveForQuery(skuIds);

        // 构建映射表
        return skuList.stream()
                .collect(Collectors.toMap(
                        Sku::getSkuCode,
                        sku -> buildSkuBriefVO(sku, imageUrlMap),
                        (v1, v2) -> v1
                ));
    }

    /**
     * 构建 SkuBriefVO
     */
    private SkuBriefVO buildSkuBriefVO(Sku sku, Map<Long, String> imageUrlMap) {
        SkuBriefVO vo = new SkuBriefVO();
        vo.setSkuCode(sku.getSkuCode());
        vo.setSkuName(StringUtils.hasText(sku.getChineseName())
                ? sku.getChineseName() : sku.getRussianName());
        vo.setMainImage(imageUrlMap.get(sku.getId()));
		vo.setSkuNo(sku.getSkuNo());
        return vo;
    }
}
