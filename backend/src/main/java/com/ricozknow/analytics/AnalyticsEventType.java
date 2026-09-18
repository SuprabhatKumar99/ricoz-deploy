package com.ricozknow.analytics;

/**
 * Fixed event vocabulary from spec section 16. Full aggregation, dashboards,
 * and knowledge-gap scoring are built in Phase 5 — this module currently only
 * owns durable, immutable event capture so Phase 3 (portal views/feedback)
 * and Phase 4 (zero-result search) have somewhere correct to write to.
 */
/**
 * Fixed event vocabulary from spec section 16. SEARCH_RESULT_CLICKED is now
 * actually emitted (Phase 8) — see PortalController's /search/click endpoint
 * and AnalyticsAggregationService, which uses it as the precise signal for
 * "unsuccessful search" instead of the earlier view-within-window proxy.
 */
public enum AnalyticsEventType {
    SEARCH_PERFORMED,
    SEARCH_RESULT_CLICKED,
    ARTICLE_VIEWED,
    ARTICLE_HELPFUL,
    ARTICLE_UNHELPFUL,
    SUPPORT_CONTACT_STARTED,
    DEFLECTION_RECORDED
}
