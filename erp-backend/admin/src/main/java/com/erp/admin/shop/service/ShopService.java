package com.erp.admin.shop.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.shop.mapper.ShopMapper;
import com.erp.admin.shop.model.dto.CreateOrUpdateShopRequest;
import com.erp.admin.shop.model.dto.TestCacheItem;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.model.enums.ShopStatusEnum;
import com.erp.admin.shop.model.qo.ShopQO;
import com.erp.admin.shop.model.vo.ShopDetailVO;
import com.erp.admin.shop.model.vo.ShopPageVO;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.util.JsonUtils;
import org.ballcat.common.util.json.TypeReference;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ShopService extends ExtendServiceImpl<ShopMapper, Shop> {

	private final CredentialService credentialService;

	public PageResult<ShopPageVO> queryPage(PageParam pageParam, ShopQO qo) {
		return baseMapper.queryPage(pageParam, qo);
	}

	public List<Shop> listEnabledByPlatform(String platform) {
		if (!StringUtils.hasText(platform)) {
			return Collections.emptyList();
		}
		// C2：取某平台【全部租户】启用店铺，是合法的平台级跨租户扫描（后台同步任务无租户上下文调用），
		// 显式放行租户注入，否则 fail-closed 后 shop 表被注入 -999 → 0 行、同步拉不到店铺。
		return TenantContext.runAsPlatformScan(() -> this.baseMapper.listEnabledByPlatform(platform));
	}

	public Shop getFirstEnabledShopByPlatform(String platform) {
		if (!StringUtils.hasText(platform)) {
			return null;
		}
		// C2：同上，平台级跨租户查询，显式放行。
		return TenantContext.runAsPlatformScan(
				() -> baseMapper.selectFirstEnabledByPlatform(platform, ShopStatusEnum.ENABLED.getCode()));
	}

	public Long createByTestResult(CreateOrUpdateShopRequest req, TestCacheItem item) {
		if (item == null) {
			throw new IllegalArgumentException("需要先测试凭证");
		}

		// 以测试结果中的平台+店铺ID做唯一性校验，避免前端绕过测试结果篡改。
		boolean exists = this.baseMapper.exists(item.getPlatform(), item.getPlatformShopId());
		if (exists) {
			throw new IllegalArgumentException("该平台店铺已存在");
		}

		Shop shop = new Shop();
		shop.setPlatform(item.getPlatform());
		shop.setPlatformShopId(item.getPlatformShopId());
		shop.setName(item.getShopName());
		shop.setErpShopName(req.getErpShopName());
		shop.setStatus(ShopStatusEnum.ENABLED.getCode());
		shop.setCredential(JsonUtils.toJson(item.getCredential()));
		shop.setLastTestedAt(LocalDateTime.now());
		shop.setCreateTime(LocalDateTime.now());
		shop.setUpdateTime(LocalDateTime.now());
		this.save(shop);
		return shop.getId();
	}

	public void updateByTestResult(Shop db, CreateOrUpdateShopRequest req, boolean credentialChanged, TestCacheItem testItem) {
		if (db == null) {
			throw new IllegalArgumentException("店铺不存在");
		}

		if (credentialChanged) {
			if (testItem == null) {
				throw new IllegalArgumentException("凭证修改后需重新测试");
			}
			// 凭证变更时，关键身份字段统一回填测试结果，防止与真实凭证不一致。
			db.setCredential(JsonUtils.toJson(testItem.getCredential()));
			db.setLastTestedAt(LocalDateTime.now());
			db.setName(testItem.getShopName());
			db.setPlatformShopId(testItem.getPlatformShopId());
		} else {
			db.setName(req.getShopName());
			db.setPlatformShopId(req.getPlatformShopId());
		}

		db.setErpShopName(req.getErpShopName());
		db.setUpdateTime(LocalDateTime.now());
		this.updateById(db);
	}

	public void toggleStatus(Long id, Integer status) {
		Shop db = this.getById(id);
		if (db == null) {
			throw new IllegalArgumentException("店铺不存在");
		}
		if (status == ShopStatusEnum.ENABLED.getCode() && db.getLastTestedAt() == null) {
			throw new IllegalArgumentException("启用前需成功测试凭证");
		}
		db.setStatus(status);
		this.updateById(db);
	}

	public ShopDetailVO detail(Long id) {
		Shop db = this.getById(id);
		if (db == null) {
			throw new IllegalArgumentException("店铺不存在");
		}
		ShopDetailVO vo = new ShopDetailVO();
		vo.setId(db.getId());
		vo.setPlatform(db.getPlatform());
		vo.setName(db.getName());
		vo.setErpShopName(db.getErpShopName());
		vo.setPlatformShopId(db.getPlatformShopId());
		vo.setStatus(db.getStatus());
		vo.setLastTestedAt(db.getLastTestedAt());
		vo.setLastTestStatus(db.getLastTestedAt() != null ? 1 : 0);
		Map<String, String> credMap = JsonUtils.toObj(db.getCredential(), new TypeReference<Map<String, String>>() {
		});
		vo.setCredentialMask(credentialService.mask(db.getPlatform(), credMap));
		return vo;
	}

	/**
	 * 填充店铺名称
	 *
	 * @param voList          订单 VO 列表
	 * @param shopIdExtractor 店铺 ID 提取函数
	 * @param shopNameSetter  店铺名称设置函数
	 * @param <T>             VO 类型
	 */
	public <T> void enrichShopName(List<T> voList, Function<T, Long> shopIdExtractor, BiConsumer<T, String> shopNameSetter) {
		if (voList == null || voList.isEmpty()) {
			return;
		}

		Set<Long> shopIds = voList.stream().map(shopIdExtractor).filter(Objects::nonNull).collect(Collectors.toSet());
		if (shopIds.isEmpty()) {
			return;
		}

		List<Shop> shops = this.baseMapper.selectBatchIds(shopIds);
		Map<Long, Shop> shopMap = shops.stream().collect(Collectors.toMap(Shop::getId, Function.identity(), (old, now) -> old));

		for (T vo : voList) {
			Long shopId = shopIdExtractor.apply(vo);
			if (shopId != null) {
				Shop shop = shopMap.get(shopId);
				if (shop != null) {
					shopNameSetter.accept(vo, shop.getErpShopName());
				}
			}
		}
	}

}
