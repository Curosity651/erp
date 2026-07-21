package com.erp.admin.wms;

import com.erp.admin.wms.service.WmsLocationGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 库位生成算法测试（C1）：二维「排×列」编码规则、数量。
 *
 * @author erp
 */
class WmsLocationGeneratorTest {

	@Test
	void location_code_pads_column() {
		// 前缀A、排1、列3、补零2 → A1-03
		assertThat(WmsLocationGenerator.buildLocationCode("A", 1, 3, 2)).isEqualTo("A1-03");
		// 排2、列10、补零2 → A2-10
		assertThat(WmsLocationGenerator.buildLocationCode("A", 2, 10, 2)).isEqualTo("A2-10");
	}

	@Test
	void empty_prefix_uses_pure_number() {
		assertThat(WmsLocationGenerator.buildLocationCode("", 1, 1, 2)).isEqualTo("1-01");
		assertThat(WmsLocationGenerator.buildLocationCode(null, 5, 4, 3)).isEqualTo("5-004");
	}

	@Test
	void count_is_rows_times_columns() {
		// 3排 × 4列 = 12
		assertThat(WmsLocationGenerator.countLocations(3, 4)).isEqualTo(12);
		assertThat(WmsLocationGenerator.countLocations(10, 10)).isEqualTo(100);
	}

}
