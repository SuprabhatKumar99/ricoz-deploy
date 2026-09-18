package com.ricozknow.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method whose successful completion should produce an audit_logs
 * entry. Use this for straightforward "action succeeded" cases; call AuditService
 * directly when you need before/after state diffing (e.g. publishing a version).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditable {
    String entityType();
    String action();
}
