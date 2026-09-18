package com.ricozknow.tenant;

import com.ricozknow.common.TenantContext;
import com.ricozknow.role.RoleName;
import com.ricozknow.role.RoleRepository;
import com.ricozknow.user.User;
import com.ricozknow.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mandatory cross-tenant access test per spec section 22: two tenants are
 * provisioned, a user is created in each, and we assert that querying by
 * tenant never returns the other tenant's data — even before RLS kicks in,
 * the application-level repository queries must already be tenant-scoped.
 */
@Testcontainers
@SpringBootTest
class TenantIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ricozknow_test")
            .withUsername("ricozknow")
            .withPassword("ricozknow");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TenantProvisioningService tenantProvisioningService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    @Test
    void usersAreNotVisibleAcrossTenants() {
        Tenant acme = tenantProvisioningService.provision("Acme", "acme", "acme");
        Tenant globex = tenantProvisioningService.provision("Globex", "globex", "globex");

        TenantContext.set(acme.getId());
        User acmeUser = new User();
        acmeUser.setEmail("admin@acme.test");
        acmeUser.setPasswordHash("irrelevant-for-this-test");
        acmeUser.setName("Acme Admin");
        userRepository.save(acmeUser);
        TenantContext.clear();

        // Querying Globex's tenant must never surface Acme's user.
        assertThat(userRepository.findByTenantIdAndEmailIgnoreCase(globex.getId(), "admin@acme.test"))
                .isEmpty();
        assertThat(userRepository.findByTenantIdAndEmailIgnoreCase(acme.getId(), "admin@acme.test"))
                .isPresent();

        assertThat(roleRepository.findByTenantId(acme.getId())).hasSize(RoleName.values().length);
        assertThat(roleRepository.findByTenantId(globex.getId())).hasSize(RoleName.values().length);
    }
}
