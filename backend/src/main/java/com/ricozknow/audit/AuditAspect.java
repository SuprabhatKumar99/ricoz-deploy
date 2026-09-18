package com.ricozknow.audit;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @AfterReturning(pointcut = "@annotation(com.ricozknow.audit.Auditable)", returning = "result")
    public void auditAnnotatedMethod(org.aspectj.lang.JoinPoint joinPoint, Object result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Auditable annotation = method.getAnnotation(Auditable.class);

        UUID entityId = extractEntityId(result);
        auditService.record(annotation.entityType(), entityId, annotation.action(), null, result);
    }

    /** Best-effort: if the returned object exposes getId(): UUID, use it as entity_id. */
    private UUID extractEntityId(Object result) {
        if (result == null) {
            return null;
        }
        try {
            Method getId = result.getClass().getMethod("getId");
            Object id = getId.invoke(result);
            return id instanceof UUID uuid ? uuid : null;
        } catch (Exception ex) {
            return null;
        }
    }
}
