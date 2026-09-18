package com.ricozknow.auth;

import com.ricozknow.audit.AuditService;
import com.ricozknow.auth.dto.LoginRequest;
import com.ricozknow.auth.dto.LoginResponse;
import com.ricozknow.auth.dto.LoginResult;
import com.ricozknow.auth.dto.RegisterRequest;
import com.ricozknow.auth.dto.RegisterResponse;
import com.ricozknow.common.TenantContext;
import com.ricozknow.role.Role;
import com.ricozknow.role.RoleName;
import com.ricozknow.role.RoleRepository;
import com.ricozknow.config.AppMetrics;
import com.ricozknow.tenant.Tenant;
import com.ricozknow.tenant.TenantRepository;
import com.ricozknow.user.AuthenticatedUser;
import com.ricozknow.user.User;
import com.ricozknow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter rateLimiter;
    private final AuditService auditService;
    private final AppMetrics appMetrics;
    private final RoleRepository roleRepository;

    private static final int MAX_FAILED_ATTEMPTS_BEFORE_LOCK = 10;

    @Transactional
    public LoginResult login(LoginRequest request, String clientIp) {
        if (rateLimiter.isBlocked(request.tenantSlug(), request.email(), clientIp)) {
            throw new AuthException("Too many login attempts. Try again later.");
        }

        Tenant tenant = tenantRepository.findBySlug(request.tenantSlug())
                .filter(t -> t.getStatus() == Tenant.TenantStatus.ACTIVE)
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        TenantContext.set(tenant.getId());

        try {
            User user = userRepository
                    .findByTenantIdAndEmailIgnoreCase(tenant.getId(), request.email())
                    .orElseThrow(() -> recordFailureAndFail(request, clientIp));

            if (user.getStatus() != User.UserStatus.ACTIVE) {
                throw new AuthException("Account is not active");
            }

            if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

                if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS_BEFORE_LOCK) {
                    user.setStatus(User.UserStatus.DISABLED);

                    auditService.record(
                            "User",
                            user.getId(),
                            "ACCOUNT_LOCKED",
                            null,
                            null
                    );
                }

                userRepository.save(user);
                throw recordFailureAndFail(request, clientIp);
            }

            user.setFailedLoginAttempts(0);
            userRepository.save(user);

            rateLimiter.recordSuccess(
                    request.tenantSlug(),
                    request.email(),
                    clientIp
            );

            AuthenticatedUser principal = AuthenticatedUser.from(user);

            String accessToken = jwtService.issueAccessToken(principal);

            String refreshToken =
                    refreshTokenService.issue(tenant.getId(), user.getId());

            auditService.record(
                    "User",
                    user.getId(),
                    "LOGIN_SUCCESS",
                    null,
                    null
            );

            appMetrics.recordLoginSuccess();

            var response = new LoginResponse(
                    accessToken,
                    user.getId(),
                    tenant.getId(),
                    user.getEmail(),
                    principal.roleNames()
            );

            return new LoginResult(response, refreshToken);

        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public LoginResult refresh(String presentedRefreshToken) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(presentedRefreshToken);

        User user = userRepository.findById(rotation.userId())
                .filter(u -> u.getStatus() == User.UserStatus.ACTIVE)
                .orElseThrow(() -> new AuthException("Account no longer active"));

        AuthenticatedUser principal = AuthenticatedUser.from(user);
        String accessToken = jwtService.issueAccessToken(principal);

        var response = new LoginResponse(
                accessToken, user.getId(), rotation.tenantId(), user.getEmail(), principal.roleNames());
        return new LoginResult(response, rotation.newPlaintextToken());
    }

    @Transactional
    public void logout(String presentedRefreshToken) {
        if (presentedRefreshToken != null) {
            refreshTokenService.revoke(presentedRefreshToken);
        }
    }


    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String slug = request.tenantSlug().trim().toLowerCase();
        String subdomain = request.subdomain().trim().toLowerCase();
        String email = request.email().trim().toLowerCase();

        if (tenantRepository.findBySlug(slug).isPresent()) {
            throw new AuthException("Tenant slug is already registered");
        }

        if (tenantRepository.findBySubdomain(subdomain).isPresent()) {
            throw new AuthException("Subdomain is already registered");
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.organizationName().trim());
        tenant.setSlug(slug);
        tenant.setSubdomain(subdomain);
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);

        tenant = tenantRepository.save(tenant);

        TenantContext.set(tenant.getId());

        try {
            Role adminRole = null;

            for (RoleName roleName : RoleName.values()) {
                Role role = roleRepository.save(new Role(roleName));

                if (roleName == RoleName.ADMIN) {
                    adminRole = role;
                }
            }

            if (adminRole == null) {
                throw new AuthException("ADMIN role could not be created");
            }

            User user = new User();
            user.setEmail(email);
            user.setName(request.name().trim());
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setStatus(User.UserStatus.ACTIVE);
            user.setFailedLoginAttempts(0);
            user.getRoles().add(adminRole);

            user = userRepository.save(user);

            auditService.record(
                    "User",
                    user.getId(),
                    "USER_REGISTERED",
                    null,
                    user
            );

            auditService.record(
                    "Tenant",
                    tenant.getId(),
                    "TENANT_REGISTERED",
                    null,
                    tenant
            );

            return new RegisterResponse(
                    tenant.getId(),
                    user.getId(),
                    tenant.getSlug(),
                    user.getEmail(),
                    RoleName.ADMIN.name(),
                    "Registration successful"
            );

        } finally {
            TenantContext.clear();
        }
    }

    private AuthException recordFailureAndFail(LoginRequest request, String clientIp) {
        rateLimiter.recordFailure(request.tenantSlug(), request.email(), clientIp);
        appMetrics.recordLoginFailure();
        return new AuthException("Invalid credentials");
    }
}