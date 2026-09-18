package com.ricozknow.role;

/**
 * Fixed MVP role set (per spec section 6). Every tenant gets these four roles
 * seeded on creation; custom/dynamic roles are out of scope for MVP.
 */
public enum RoleName {
    ADMIN,
    EDITOR,
    REVIEWER,
    AGENT_VIEWER
}
