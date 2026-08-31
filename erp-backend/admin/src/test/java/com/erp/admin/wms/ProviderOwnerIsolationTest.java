package com.erp.admin.wms;

import com.erp.admin.tenant.exception.TenantBusinessException;
import com.erp.admin.tenant.mapper.ErpAccountMapper;
import com.erp.admin.tenant.mapper.ErpUserRoleMapper;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.mapper.TenantRoleMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.tenant.service.TenantProvisionService;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderOwnerIsolationTest {

    @Test
    void provider_owner_query_is_scoped_to_current_provider() throws Exception {
        Fixture fixture = fixture(identity(101L, true));
        when(fixture.tenantMapper.selectList(any())).thenReturn(Collections.emptyList());

        fixture.service.listErpTenants();

        verify(fixture.tenantMapper).selectList(any());
        String source = new String(Files.readAllBytes(Paths.get(
            "src/main/java/com/erp/admin/tenant/service/TenantProvisionService.java")),
            StandardCharsets.UTF_8);
        assertThat(source).contains(".eq(SysTenant::getParentWmsTenantId, identity.getTenantId())");
    }

    @Test
    void provider_cannot_disable_another_providers_owner() {
        Fixture fixture = fixture(identity(101L, true));
        SysTenant foreignOwner = owner(301L, 202L, 1);
        when(fixture.tenantMapper.selectById(301L)).thenReturn(foreignOwner);

        assertThatThrownBy(() -> fixture.service.setErpTenantStatus(301L, 0))
            .isInstanceOf(TenantBusinessException.class)
            .hasMessageContaining("无权操作");
        verify(fixture.tenantMapper, never()).updateById(any(SysTenant.class));
    }

    @Test
    void non_admin_provider_cannot_disable_its_owner() {
        Fixture fixture = fixture(identity(101L, false));

        assertThatThrownBy(() -> fixture.service.setErpTenantStatus(301L, 0))
            .isInstanceOf(TenantBusinessException.class);
        verify(fixture.tenantMapper, never()).selectById(301L);
    }

    @Test
    void disabled_owner_is_rejected_on_next_identity_resolution() {
        PrincipalAttributeAccessor principal = mock(PrincipalAttributeAccessor.class);
        ErpAccountMapper accountMapper = mock(ErpAccountMapper.class);
        SysTenantMapper tenantMapper = mock(SysTenantMapper.class);
        when(principal.getUserId()).thenReturn(9L);
        when(accountMapper.selectTenantIdByUserId(9L)).thenReturn(301L);
        when(tenantMapper.selectById(301L)).thenReturn(owner(301L, 101L, 0));
        TenantIdentityService identityService = new TenantIdentityService(principal, accountMapper, tenantMapper);

        assertThatThrownBy(() -> identityService.currentIdentity(null))
            .isInstanceOf(TenantBusinessException.class);
    }

    private static Fixture fixture(TenantIdentityVO identity) {
        TenantIdentityService identityService = mock(TenantIdentityService.class);
        SysTenantMapper tenantMapper = mock(SysTenantMapper.class);
        when(identityService.currentIdentity(null)).thenReturn(identity);
        TenantProvisionService service = new TenantProvisionService(
            identityService,
            tenantMapper,
            mock(ErpUserRoleMapper.class),
            mock(ErpAccountMapper.class),
            mock(TenantRoleMapper.class),
            mock(PasswordEncoder.class)
        );
        return new Fixture(service, tenantMapper);
    }

    private static TenantIdentityVO identity(Long tenantId, boolean admin) {
        TenantIdentityVO identity = new TenantIdentityVO();
        identity.setTenantId(tenantId);
        identity.setTenantType(TenantIdentityService.IDENTITY_WMS_OPERATOR);
        identity.setIdentityType(TenantIdentityService.IDENTITY_WMS_OPERATOR);
        identity.setAdmin(admin);
        return identity;
    }

    private static SysTenant owner(Long id, Long parentId, Integer status) {
        SysTenant owner = new SysTenant();
        owner.setId(id);
        owner.setTenantType(TenantIdentityService.IDENTITY_ERP_USER);
        owner.setParentWmsTenantId(parentId);
        owner.setStatus(status);
        return owner;
    }

    private static final class Fixture {
        private final TenantProvisionService service;
        private final SysTenantMapper tenantMapper;

        private Fixture(TenantProvisionService service, SysTenantMapper tenantMapper) {
            this.service = service;
            this.tenantMapper = tenantMapper;
        }
    }
}
