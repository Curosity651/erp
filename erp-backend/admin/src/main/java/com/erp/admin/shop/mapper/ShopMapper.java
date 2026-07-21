package com.erp.admin.shop.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.shop.converter.ShopConverter;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.model.enums.ShopStatusEnum;
import com.erp.admin.shop.model.qo.ShopQO;
import com.erp.admin.shop.model.vo.ShopPageVO;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * 店铺（含凭证信息）
 *
 * @author erp 2025-09-21 21:11:47
 */
public interface ShopMapper extends ExtendMapper<Shop> {

	/**
	 * 分页查询
	 *
	 * @param pageParam 分页参数
	 * @param qo        查询参数
	 * @return PageResult<ShopPageVO> VO分页数据
	 */
	default PageResult<ShopPageVO> queryPage(PageParam pageParam, ShopQO qo) {
		IPage<Shop> page = this.prodPage(pageParam);
		LambdaQueryWrapperX<Shop> wrapper = WrappersX.lambdaQueryX(Shop.class)
				.eqIfPresent(Shop::getPlatform, qo.getPlatform())
				.eqIfPresent(Shop::getStatus, qo.getStatus())
				.and(qo.getKeyword() != null && !qo.getKeyword().isEmpty(),
						w -> w.like(Shop::getName, qo.getKeyword()).or().like(Shop::getPlatformShopId, qo.getKeyword()))
				.orderByDesc(Shop::getUpdateTime, Shop::getId);
		this.selectPage(page, wrapper);
		IPage<ShopPageVO> voPage = page.convert(ShopConverter.INSTANCE::poToPageVo);
		return new PageResult<>(voPage.getRecords(), voPage.getTotal());
	}

	/**
	 * 获取指定平台第一个启用的店铺
	 * @param platform 平台
	 * @param status 状态
	 * @return 店铺实体，不存在返回 null
	 */
	default Shop selectFirstEnabledByPlatform(String platform, Integer status) {
		LambdaQueryWrapperX<Shop> wrapper = WrappersX.lambdaQueryX(Shop.class)
				.eq(Shop::getPlatform, platform)
				.eq(Shop::getStatus, status)
				.last("LIMIT 1");
		return this.selectOne(wrapper);
	}

	/**
	 * 获取指定平台所有启用的店铺
	 * @param platform 平台
	 * @return 店铺实体列表
	 */
	default List<Shop> listEnabledByPlatform(String platform) {
		LambdaQueryWrapperX<Shop> wrapper = WrappersX.lambdaQueryX(Shop.class)
				.eq(Shop::getPlatform, platform)
				.eq(Shop::getStatus, ShopStatusEnum.ENABLED.getCode());
		return this.selectList(wrapper);
	}

	/**
	 * 判断是否存在
	 */
	default boolean exists(String platform, String platformShopId) {
		Long cnt = this.selectCount(WrappersX.lambdaQueryX(Shop.class)
				.eq(Shop::getPlatform, platform)
				.eq(Shop::getPlatformShopId, platformShopId));
		return cnt != null && cnt > 0;
	}


}