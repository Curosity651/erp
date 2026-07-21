package com.erp.admin.shop.facade;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.platform.credential.CredentialTestResult;
import com.erp.admin.shop.model.dto.CreateOrUpdateShopRequest;
import com.erp.admin.shop.model.dto.TestCacheItem;
import com.erp.admin.shop.model.dto.TestCredentialRequest;
import com.erp.admin.shop.model.dto.TestCredentialResponse;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.model.enums.ShopStatusEnum;
import com.erp.admin.shop.model.qo.ShopQO;
import com.erp.admin.shop.model.vo.ShopDetailVO;
import com.erp.admin.shop.model.vo.ShopPageVO;
import com.erp.admin.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.util.JsonUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ShopFacade {

	private final ShopService shopService;

	private final CredentialService credentialService;

	/**
	 * 简单内存缓存: token -> TestCacheItem (10 分钟有效)
	 */
	private static final Map<String, TestCacheItem> TEST_CACHE = new ConcurrentHashMap<>();
	private static final long TEST_CACHE_TTL_SECONDS = 600L;

	public PageResult<ShopPageVO> queryPage(PageParam pageParam, ShopQO qo) {
		return shopService.queryPage(pageParam, qo);
	}

	public ShopDetailVO detail(Long id) {
		return shopService.detail(id);
	}

	public void toggleStatus(Long id, Integer status) {
		shopService.toggleStatus(id, status);
	}

	public TestCredentialResponse testCredential(TestCredentialRequest req) {
		if (!StringUtils.hasText(req.getPlatform()) || req.getCredential() == null) {
			throw new IllegalArgumentException("platform/credential 不能为空");
		}

		CredentialTestResult testResult = credentialService.validate(req.getPlatform(), req.getCredential());

		String tokenSeed = req.getPlatform() + System.nanoTime();
		String token = DigestUtils.md5DigestAsHex(tokenSeed.getBytes());

		TestCacheItem item = new TestCacheItem();
		item.setPlatform(req.getPlatform());
		item.setCredential(new HashMap<>(req.getCredential()));
		item.setShopName(testResult.getShopName());
		item.setPlatformShopId(testResult.getPlatformShopId());
		item.setTestedAt(LocalDateTime.now());

		TEST_CACHE.put(token, item);
		cleanupTestCache();

		TestCredentialResponse resp = new TestCredentialResponse();
		resp.setPlatform(req.getPlatform());
		resp.setShopName(testResult.getShopName());
		resp.setPlatformShopId(testResult.getPlatformShopId());
		resp.setTestToken(token);
		resp.setShopNameRequired(testResult.isShopNameRequired());
		return resp;
	}

	public Long create(CreateOrUpdateShopRequest req) {
		if (!StringUtils.hasText(req.getTestToken())) {
			throw new IllegalArgumentException("需要先测试凭证");
		}

		TestCacheItem item = consumeValidToken(req.getTestToken(), req.getPlatform(), req.getCredential(), req.getPlatformShopId(), req.getShopName());

		return shopService.createByTestResult(req, item);
	}

	public void update(Long id, CreateOrUpdateShopRequest req) {
		Shop db = shopService.getById(id);
		if (db == null) {
			throw new IllegalArgumentException("店铺不存在");
		}

		if (StringUtils.hasText(req.getPlatform()) && !req.getPlatform().equalsIgnoreCase(db.getPlatform())) {
			throw new IllegalArgumentException("不允许修改平台");
		}

		boolean credentialChanged = req.getCredential() != null && !req.getCredential().isEmpty() && !Objects.equals(db.getCredential(), JsonUtils.toJson(req.getCredential()));

		TestCacheItem item = null;
		if (credentialChanged) {
			// 凭证变化必须重新测试，后续以测试结果为准写入关键字段。
			if (!StringUtils.hasText(req.getTestToken())) {
				throw new IllegalArgumentException("凭证修改后需重新测试");
			}
			item = consumeValidToken(req.getTestToken(), db.getPlatform(), req.getCredential(), req.getPlatformShopId(), req.getShopName());
		}

		shopService.updateByTestResult(db, req, credentialChanged, item);
	}

	public void retest(Long id) {
		Shop shop = shopService.getById(id);
		if (shop == null) {
			throw new IllegalArgumentException("店铺不存在");
		}

		try {
			CredentialTestResult testResult = credentialService.revalidate(shop);

			shop.setLastTestedAt(LocalDateTime.now());
			if (testResult.getShopName() != null) {
				shop.setName(testResult.getShopName());
			}
			if (testResult.getPlatformShopId() != null) {
				shop.setPlatformShopId(testResult.getPlatformShopId());
			}
			if (Objects.equals(shop.getStatus(), ShopStatusEnum.ABNORMAL.getCode())) {
				shop.setStatus(ShopStatusEnum.ENABLED.getCode());
			}
			shopService.updateById(shop);
		} catch (Exception ex) {
			if (!Objects.equals(shop.getStatus(), ShopStatusEnum.DISABLED.getCode())) {
				shop.setStatus(ShopStatusEnum.ABNORMAL.getCode());
				shopService.updateById(shop);
			}
			throw new IllegalArgumentException("重新测试失败: " + ex.getMessage());
		}
	}

	private TestCacheItem consumeValidToken(String token, String platform, Map<String, String> credential, String platformShopId, String shopName) {
		cleanupTestCache();
		// 一次性消费，避免同一个 token 被重复提交重放。
		TestCacheItem item = TEST_CACHE.remove(token);
		if (item == null) {
			throw new IllegalArgumentException("testToken 无效或已过期");
		}

		if (!item.getPlatform().equalsIgnoreCase(platform)) {
			throw new IllegalArgumentException("testToken 与提交数据不匹配(平台)");
		}

		if (!Objects.equals(item.getCredential(), credential)) {
			throw new IllegalArgumentException("凭证已修改需重新测试");
		}

		if (PlatformEnum.Ozon.code().equalsIgnoreCase(platform) || PlatformEnum.Yandex.code().equalsIgnoreCase(platform)) {
			// Ozon/Yandex 无法稳定返回店铺名，名称由用户确认并提交。
			String idField = PlatformEnum.Ozon.code().equalsIgnoreCase(platform) ? "client_id" : "campaign_id";
			String idValue = credential.get(idField);
			if (!StringUtils.hasText(idValue)) {
				throw new IllegalArgumentException(platform + " 平台缺少 " + idField);
			}
			if (!StringUtils.hasText(shopName)) {
				throw new IllegalArgumentException(platform + " 平台需要手动填写店铺名称");
			}
			if (!Objects.equals(idValue, platformShopId)) {
				throw new IllegalArgumentException(platform + " 平台店铺ID不匹配");
			}
			item.setShopName(shopName);
			item.setPlatformShopId(platformShopId);
		} else {
			// Wildberries 测试阶段可拿到店铺信息，创建/更新时必须严格一致。
			if (!Objects.equals(item.getPlatformShopId(), platformShopId)) {
				throw new IllegalArgumentException("店铺ID与测试结果不一致");
			}
			if (!Objects.equals(item.getShopName(), shopName)) {
				throw new IllegalArgumentException("店铺名称与测试结果不一致");
			}
		}

		return item;
	}

	private void cleanupTestCache() {
		LocalDateTime now = LocalDateTime.now();
		TEST_CACHE.entrySet().removeIf(e -> e.getValue().getTestedAt().plusSeconds(TEST_CACHE_TTL_SECONDS).isBefore(now));
	}

}
