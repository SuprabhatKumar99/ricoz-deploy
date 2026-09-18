package com.ricozknow.audit;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ricozknow.common.TenantContext;
import com.ricozknow.user.AuthenticatedUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Audit logging is best-effort.
     *
     * REQUIRES_NEW is intentionally retained for normal audit events,
     * but saveAndFlush() is used so database errors happen inside the
     * try/catch rather than during transaction commit.
     */
   @Transactional
    public void record(
            String entityType,
            UUID entityId,
            String action,
            Object oldState,
            Object newState
    ) {
        try {
            AuditLog auditLog = new AuditLog();

            auditLog.setTenantId(resolveTenantId());
            auditLog.setActorUserId(resolveActorUserId());
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setAction(action);

            auditLog.setOldState(
                    oldState != null
                            ? objectMapper.writeValueAsString(oldState)
                            : null
            );

            auditLog.setNewState(
                    newState != null
                            ? objectMapper.writeValueAsString(newState)
                            : null
            );

            auditLogRepository.save(auditLog);

        } catch (Exception ex) {
            log.error(
                    "Failed to write audit log for {}#{} action={}",
                    entityType,
                    entityId,
                    action,
                    ex
            );
        }
    }

    private UUID resolveTenantId() {

        UUID tenantId = TenantContext.getOrNull();

        if (tenantId != null) {
            return tenantId;
        }

        var authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication != null
                && authentication.getPrincipal()
                instanceof AuthenticatedUser principal) {

            return principal.tenantId();
        }

        throw new IllegalStateException(
                "Cannot audit-log without a resolvable tenant"
        );
    }

    private UUID resolveActorUserId() {

        var authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication != null
                && authentication.getPrincipal()
                instanceof AuthenticatedUser principal) {

            return principal.userId();
        }

        // Registration/system operation has no authenticated user yet.
        return null;
    }
}