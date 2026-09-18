package com.ricozknow.user;

import com.ricozknow.audit.AuditService;
import com.ricozknow.common.TenantContext;
import com.ricozknow.role.Role;
import com.ricozknow.role.RoleName;
import com.ricozknow.role.RoleRepository;
import com.ricozknow.user.dto.CreateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Covers both tenant staff accounts (ADMIN/EDITOR/REVIEWER) and the
 * "service account" pattern the Agent API relies on: an AGENT_VIEWER user
 * created here, whose credentials an external CRM/support tool then uses
 * against the normal /api/v1/auth/login endpoint (spec section 19's Agent
 * API deliberately reuses the existing auth model rather than inventing a
 * separate API-key scheme not called for anywhere in the spec).
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<User> list() {
        // Simple tenant-scoped listing; pagination can be added if tenant staff
        // rosters grow large enough to need it.
        return userRepository.findAll().stream()
                .filter(u -> u.getTenantId().equals(TenantContext.get()))
                .toList();
    }

    @Transactional
    public User create(CreateUserRequest request) {
        UUID tenantId = TenantContext.get();
        if (userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, request.email()).isPresent()) {
            throw new IllegalStateException("A user with email " + request.email() + " already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setName(request.name());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(User.UserStatus.ACTIVE);

        Set<Role> roles = new HashSet<>();
        for (RoleName roleName : request.roles()) {
            roles.add(roleRepository.findByTenantIdAndName(tenantId, roleName)
                    .orElseThrow(() -> new IllegalStateException("Role not seeded for tenant: " + roleName)));
        }
        user.setRoles(roles);

        user = userRepository.save(user);
        auditService.record("User", user.getId(), "USER_CREATED", null, request.email());
        return user;
    }

    @Transactional
    public void disable(UUID userId) {
        User user = userRepository.findById(userId)
                .filter(u -> u.getTenantId().equals(TenantContext.get()))
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        user.setStatus(User.UserStatus.DISABLED);
        auditService.record("User", userId, "USER_DISABLED", null, null);
    }
}
