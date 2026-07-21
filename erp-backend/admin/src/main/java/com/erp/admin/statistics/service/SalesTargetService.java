package com.erp.admin.statistics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.dto.MonthlyAmountDTO;
import com.erp.admin.statistics.converter.SalesTargetConverter;
import com.erp.admin.statistics.mapper.SalesTargetMapper;
import com.erp.admin.statistics.model.SalesTarget;
import com.erp.admin.statistics.model.dto.MonthlyTargetDTO;
import com.erp.admin.statistics.model.dto.SalesTargetBatchDTO;
import com.erp.admin.statistics.model.dto.SalesTargetBatchMonthlyUpdateDTO;
import com.erp.admin.statistics.model.dto.SalesTargetUpdateDTO;
import com.erp.admin.statistics.model.entity.enums.TargetType;
import com.erp.admin.statistics.model.vo.SalesTargetPageVO;
import com.erp.admin.statistics.model.vo.SalesTargetYearlyOverviewVO;
import com.erp.admin.system.model.dto.AnnualTargetDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * 销售目标
 *
 * @author erp 2025-10-25 21:40:57
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesTargetService extends ExtendServiceImpl<SalesTargetMapper, SalesTarget> {

	private final PrincipalAttributeAccessor principalAttributeAccessor;
	
	private final ErpOrderMapper erpOrderMapper;

	/**
	 * 创建单独的年度目标
	 *
	 * @param dto 年度目标创建DTO
	 */
	@Transactional(rollbackFor = Exception.class)
	public void createAnnualTarget(com.erp.admin.statistics.model.dto.AnnualTargetCreateDTO dto) {
		Integer year = dto.getYear();
		
		// 检查年度目标是否已存在
		Assert.isTrue(!baseMapper.existsYearlyTarget(year), "该年份的年度目标已存在");

		SalesTarget annualTarget = new SalesTarget();
		annualTarget.setTargetType(TargetType.YEARLY);
		annualTarget.setTargetYear(year);
		annualTarget.setTargetMonth(null);
		annualTarget.setTargetAmount(dto.getAmount());
		annualTarget.setCurrency("RUB"); // 默认货币
		annualTarget.setCreatedBy(this.principalAttributeAccessor.getUserId());
		annualTarget.setRemark(dto.getRemark());
		baseMapper.insert(annualTarget);
	}

	/**
	 * 批量保存年度和月度目标
	 *
	 * @param dto 批量创建DTO
	 */
	@Transactional(rollbackFor = Exception.class)
	public void saveYearlyWithMonthlyTargets(SalesTargetBatchDTO dto) {
		Integer year = dto.getYear();
		String currency = dto.getCurrency();
		Long currentUserId = this.principalAttributeAccessor.getUserId();

		// 保存年度目标(如果提供了且金额大于0)
		AnnualTargetDTO annualDto = dto.getAnnualTarget();
		if (annualDto != null && annualDto.getAmount() != null && annualDto.getAmount().compareTo(BigDecimal.ZERO) > 0) {
			// 检查年度目标是否已存在
			Assert.isTrue(!baseMapper.existsYearlyTarget(year), "该年份的年度目标已存在");

			SalesTarget annualTarget = new SalesTarget();
			annualTarget.setTargetType(TargetType.YEARLY);
			annualTarget.setTargetYear(year);
			annualTarget.setTargetMonth(null);
			annualTarget.setTargetAmount(annualDto.getAmount());
			annualTarget.setCurrency(currency);
			annualTarget.setCreatedBy(currentUserId);
			annualTarget.setRemark(annualDto.getRemark());
			baseMapper.insert(annualTarget);
		}

		// 批量保存月度目标
		List<MonthlyTargetDTO> monthlyDtos = dto.getMonthlyTargets();
		if (monthlyDtos != null && !monthlyDtos.isEmpty()) {
			List<SalesTarget> monthlyTargets = new ArrayList<>();
			for (MonthlyTargetDTO monthlyDto : monthlyDtos) {
				// 检查月度目标是否已存在
				Assert.isTrue(!baseMapper.existsMonthlyTarget(year, monthlyDto.getMonth()),
						"该年份的" + monthlyDto.getMonth() + "月目标已存在");

				// 只保存有金额的月度目标
				if (monthlyDto.getAmount() != null && monthlyDto.getAmount().compareTo(BigDecimal.ZERO) > 0) {
					SalesTarget monthlyTarget = new SalesTarget();
					monthlyTarget.setTargetType(TargetType.MONTHLY);
					monthlyTarget.setTargetYear(year);
					monthlyTarget.setTargetMonth(monthlyDto.getMonth());
					monthlyTarget.setTargetAmount(monthlyDto.getAmount());
					monthlyTarget.setCurrency(currency);
					monthlyTarget.setCreatedBy(currentUserId);
					monthlyTarget.setRemark(monthlyDto.getRemark());
					monthlyTargets.add(monthlyTarget);
				}
			}
			if (!monthlyTargets.isEmpty()) {
				this.saveBatch(monthlyTargets);
			}
		}
	}

	/**
	 * 获取年度概览(包含年度目标和所有月度目标)
	 *
	 * @param year 目标年份
	 * @return 年度概览VO
	 */
	public SalesTargetYearlyOverviewVO getYearlyOverview(Integer year) {
		SalesTargetYearlyOverviewVO overview = new SalesTargetYearlyOverviewVO();

		// 一次性查询全年按月分组的实际销售金额
		java.util.Map<Integer, BigDecimal> monthlyActualAmounts = new java.util.HashMap<>();
		BigDecimal annualActualTotal = BigDecimal.ZERO;
		
		try {
			List<MonthlyAmountDTO> monthlyData = erpOrderMapper.calculateMonthlyCompletedOrdersAmount(year);
			// 转换为Map并计算年度总额
			for (MonthlyAmountDTO dto : monthlyData) {
				if (dto.getMonth() != null && dto.getAmount() != null) {
					monthlyActualAmounts.put(dto.getMonth(), dto.getAmount());
					annualActualTotal = annualActualTotal.add(dto.getAmount());
				}
			}
		} catch (Exception e) {
			log.error("查询年度实际销售金额失败: year={}", year, e);
		}

		// 查询年度目标
		SalesTarget annualTarget = baseMapper.selectYearlyTargetByYear(year);
		if (annualTarget != null) {
			overview.setAnnualTarget(SalesTargetConverter.INSTANCE.poToPageVo(annualTarget));
			overview.setAnnualActualAmount(annualActualTotal);
			
			// 计算年度达成率
			if (annualTarget.getTargetAmount() != null && annualTarget.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
				BigDecimal rate = annualActualTotal.divide(annualTarget.getTargetAmount(), 4, RoundingMode.HALF_UP)
						.multiply(new BigDecimal("100"));
				overview.setAnnualAchievementRate(rate);
			}
		}

		// 查询所有月度目标
		List<SalesTarget> monthlyTargets = baseMapper.selectMonthlyTargetsByYear(year);
		List<SalesTargetPageVO> monthlyVOs = new ArrayList<>();
		BigDecimal monthlySum = BigDecimal.ZERO;

		for (SalesTarget target : monthlyTargets) {
			SalesTargetPageVO monthlyVO = SalesTargetConverter.INSTANCE.poToPageVo(target);
			
			// 从Map中获取该月的实际金额
			BigDecimal monthlyActual = monthlyActualAmounts.getOrDefault(target.getTargetMonth(), BigDecimal.ZERO);
			monthlyVO.setActualAmount(monthlyActual);
			
			// 计算月度达成率
			if (target.getTargetAmount() != null && target.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
				BigDecimal rate = monthlyActual.divide(target.getTargetAmount(), 4, RoundingMode.HALF_UP)
						.multiply(new BigDecimal("100"));
				monthlyVO.setAchievementRate(rate);
			}
			
			monthlyVOs.add(monthlyVO);
			if (target.getTargetAmount() != null) {
				monthlySum = monthlySum.add(target.getTargetAmount());
			}
		}

		overview.setMonthlyTargets(monthlyVOs);
		overview.setMonthlySum(monthlySum);
		overview.setMonthlyCount(monthlyTargets.size());

		// 计算差额
		if (annualTarget != null && annualTarget.getTargetAmount() != null) {
			overview.setDifference(annualTarget.getTargetAmount().subtract(monthlySum));
		}

		return overview;
	}

	/**
	 * 更新单个目标
	 *
	 * @param dto 更新DTO
	 */
	public void updateTarget(SalesTargetUpdateDTO dto) {
		SalesTarget target = baseMapper.selectById(dto.getId());
		Assert.notNull(target, "目标不存在");

		target.setTargetAmount(dto.getTargetAmount());
		target.setRemark(dto.getRemark());
		baseMapper.updateById(target);
	}

	/**
	 * 创建单个月度目标
	 *
	 * @param dto 创建DTO
	 */
	@Transactional(rollbackFor = Exception.class)
	public void createSingleMonthlyTarget(MonthlyTargetDTO dto) {
		// 检查年度目标是否存在
		SalesTarget annualTarget = baseMapper.selectYearlyTargetByYear(dto.getYear());
		Assert.notNull(annualTarget, "请先创建该年份的年度目标");

		// 检查月度目标是否已存在
		Assert.isTrue(!baseMapper.existsMonthlyTarget(dto.getYear(), dto.getMonth()), "该年份的" + dto.getMonth() + "月目标已存在");

		// 创建月度目标
		SalesTarget monthlyTarget = new SalesTarget();
		monthlyTarget.setTargetType(TargetType.MONTHLY);
		monthlyTarget.setTargetYear(dto.getYear());
		monthlyTarget.setTargetMonth(dto.getMonth());
		monthlyTarget.setTargetAmount(dto.getAmount());
		monthlyTarget.setCurrency(annualTarget.getCurrency());
		monthlyTarget.setCreatedBy(principalAttributeAccessor.getUserId());
		monthlyTarget.setRemark(dto.getRemark());
		baseMapper.insert(monthlyTarget);
	}

	/**
	 * 批量更新月度目标(支持更新年度目标+更新/创建月度目标)
	 *
	 * @param dto 批量更新DTO
	 */
	@Transactional(rollbackFor = Exception.class)
	public void batchUpdateMonthlyTargets(SalesTargetBatchMonthlyUpdateDTO dto) {
		Integer year = dto.getYear();
		String currency = dto.getCurrency();

		// 1. 更新年度目标(如果提供了)
		if (dto.getAnnualTargetId() != null && dto.getAnnualTarget() != null) {
			SalesTarget annualTarget = baseMapper.selectById(dto.getAnnualTargetId());
			Assert.notNull(annualTarget, "年度目标不存在");
			Assert.isTrue(annualTarget.getTargetType() == TargetType.YEARLY, "目标类型不是年度目标");

			annualTarget.setTargetAmount(dto.getAnnualTarget().getAmount());
			annualTarget.setRemark(dto.getAnnualTarget().getRemark());
			baseMapper.updateById(annualTarget);
		}

		// 2. 更新已存在的月度目标
		List<SalesTargetUpdateDTO> updateTargets = dto.getUpdateTargets();
		if (updateTargets != null && !updateTargets.isEmpty()) {
			for (SalesTargetUpdateDTO updateDto : updateTargets) {
				SalesTarget target = baseMapper.selectById(updateDto.getId());
				Assert.notNull(target, "目标ID " + updateDto.getId() + " 不存在");
				Assert.isTrue(target.getTargetType() == TargetType.MONTHLY, "只能更新月度目标");
				Assert.isTrue(target.getTargetYear().equals(year), "目标年份不匹配");

				target.setTargetAmount(updateDto.getTargetAmount());
				target.setRemark(updateDto.getRemark());
				baseMapper.updateById(target);
			}
		}

		// 3. 创建新的月度目标
		List<MonthlyTargetDTO> createTargets = dto.getCreateTargets();
		if (createTargets != null && !createTargets.isEmpty()) {
			List<SalesTarget> newTargets = new ArrayList<>();
			for (MonthlyTargetDTO createDto : createTargets) {
				// 检查是否已存在
				Assert.isTrue(!baseMapper.existsMonthlyTarget(year, createDto.getMonth()),
						"该年份的" + createDto.getMonth() + "月目标已存在");

				SalesTarget newTarget = new SalesTarget();
				newTarget.setTargetType(TargetType.MONTHLY);
				newTarget.setTargetYear(year);
				newTarget.setTargetMonth(createDto.getMonth());
				newTarget.setTargetAmount(createDto.getAmount());
				newTarget.setCurrency(currency);
				newTarget.setCreatedBy(this.principalAttributeAccessor.getUserId());
				newTarget.setRemark(createDto.getRemark());
				// 注意:这里需要设置createdBy,但我们没有用户ID
				// 可以从SecurityContext获取或者作为参数传入
				newTargets.add(newTarget);
			}
			this.saveBatch(newTargets);
		}
	}

}