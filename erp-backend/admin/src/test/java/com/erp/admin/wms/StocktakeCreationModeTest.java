package com.erp.admin.wms;

import com.erp.admin.wms.model.enums.StocktakeMode;
import com.erp.admin.wms.service.StocktakeService;
import com.erp.admin.wms.model.entity.StocktakeOrderItem;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StocktakeCreationModeTest {

    @Test
    void only_full_and_cycle_can_be_created() {
        assertThatNoException().isThrownBy(() -> StocktakeService.validateCreatableMode(StocktakeMode.FULL));
        assertThatNoException().isThrownBy(() -> StocktakeService.validateCreatableMode(StocktakeMode.CYCLE));
        assertThatThrownBy(() -> StocktakeService.validateCreatableMode(StocktakeMode.SPECIAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("专项盘点已停止新建");
    }

    @Test
    void existing_inventory_line_blocks_duplicate_extra_item() {
        StocktakeOrderItem existing = new StocktakeOrderItem();
        existing.setErpTenantId(6L);
        existing.setSkuCode("SKU-A");
        existing.setQuality("GOOD");
        existing.setSourceInventoryId(99L);

        assertThatThrownBy(() -> StocktakeService.validateNoDuplicateInventoryLine(
                Collections.singletonList(existing), 6L, "SKU-A", "GOOD"))
                .hasMessageContaining("已存在");
    }
}
