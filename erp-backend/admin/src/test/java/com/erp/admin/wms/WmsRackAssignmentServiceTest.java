package com.erp.admin.wms;

import java.time.LocalDate;

import com.erp.admin.wms.service.WmsRackAssignmentService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 货架分配纯逻辑测试（C2）：时间段重叠、有效、临期。
 *
 * @author erp
 */
class WmsRackAssignmentServiceTest {

	@Test
	void overlapping_periods_detected() {
		LocalDate jan1 = LocalDate.of(2026, 1, 1);
		LocalDate jun30 = LocalDate.of(2026, 6, 30);
		LocalDate mar1 = LocalDate.of(2026, 3, 1);
		LocalDate dec31 = LocalDate.of(2026, 12, 31);
		// [1/1,6/30] 与 [3/1,12/31] 重叠
		assertThat(WmsRackAssignmentService.periodsOverlap(jan1, jun30, mar1, dec31)).isTrue();
		// 开放结束日（null）与任何之后的段重叠
		assertThat(WmsRackAssignmentService.periodsOverlap(jan1, null, dec31, null)).isTrue();
	}

	@Test
	void non_overlapping_periods() {
		LocalDate jan1 = LocalDate.of(2026, 1, 1);
		LocalDate mar31 = LocalDate.of(2026, 3, 31);
		LocalDate apr1 = LocalDate.of(2026, 4, 1);
		LocalDate jun30 = LocalDate.of(2026, 6, 30);
		// [1/1,3/31] 与 [4/1,6/30] 不重叠
		assertThat(WmsRackAssignmentService.periodsOverlap(jan1, mar31, apr1, jun30)).isFalse();
	}

	@Test
	void active_judgement() {
		LocalDate today = LocalDate.of(2026, 6, 28);
		// 已开始、未结束
		assertThat(WmsRackAssignmentService.isActive(LocalDate.of(2026, 1, 1), null, today)).isTrue();
		// 未开始
		assertThat(WmsRackAssignmentService.isActive(LocalDate.of(2026, 7, 1), null, today)).isFalse();
		// 已结束
		assertThat(WmsRackAssignmentService.isActive(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 1), today))
			.isFalse();
	}

	@Test
	void expiring_soon_within_30_days() {
		LocalDate today = LocalDate.of(2026, 6, 1);
		// 6/20 距今 19 天 → 临期
		assertThat(WmsRackAssignmentService.isExpiringSoon(LocalDate.of(2026, 6, 20), today)).isTrue();
		// 8/1 距今 > 30 天 → 不临期
		assertThat(WmsRackAssignmentService.isExpiringSoon(LocalDate.of(2026, 8, 1), today)).isFalse();
		// 无结束日 → 不临期
		assertThat(WmsRackAssignmentService.isExpiringSoon(null, today)).isFalse();
	}

}
