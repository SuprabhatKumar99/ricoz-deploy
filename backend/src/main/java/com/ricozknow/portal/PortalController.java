// package com.ricozknow.portal;

// import com.ricozknow.portal.dto.PortalArticleDetail;
// import com.ricozknow.portal.dto.PortalArticleSummary;
// import com.ricozknow.search.SearchService;
// import com.ricozknow.search.dto.SearchResultResponse;
// import jakarta.servlet.http.HttpServletRequest;
// import lombok.RequiredArgsConstructor;
// import org.springframework.data.domain.Page;
// import org.springframework.data.domain.Pageable;
// import org.springframework.web.bind.annotation.*;

// import java.util.UUID;

// /**
//  * Everything here is anonymous-accessible (spec section 2's "Customer Portal").
//  * Tenant is resolved from the request hostname by TenantResolverFilter same as
//  * any other request — no separate tenant param needed on these endpoints.
//  */
// @RestController
// @RequestMapping("/api/v1/portal")
// @RequiredArgsConstructor
// public class PortalController {

//     private static final String SESSION_COOKIE = "rk_session";
//     private static final int SEARCH_PAGE_SIZE = 10;

//     private final PortalArticleService portalArticleService;
//     private final SearchService searchService;

//     @GetMapping("/articles")
//     public Page<PortalArticleSummary> browse(@RequestParam(required = false) UUID categoryId, Pageable pageable) {
//         return portalArticleService.browse(categoryId, pageable);
//     }

//     @GetMapping("/articles/{slug}")
//     public PortalArticleDetail get(@PathVariable String slug, HttpServletRequest request) {
//         return portalArticleService.getBySlug(slug, sessionId(request));
//     }

//     /** Real OpenSearch-backed search (Phase 4), replacing the Phase 3 naive DB stub entirely. */
//     @GetMapping("/search")
//     public SearchResultResponse search(@RequestParam String q,
//                                         @RequestParam(required = false) UUID categoryId,
//                                         @RequestParam(defaultValue = "0") int page,
//                                         HttpServletRequest request) {
//         return searchService.search(q, categoryId, page, SEARCH_PAGE_SIZE, sessionId(request));
//     }
    

//     @PostMapping("/articles/{id}/feedback")
//     public void feedback(@PathVariable UUID id, @RequestParam boolean helpful, HttpServletRequest request) {
//         portalArticleService.recordFeedback(id, helpful, sessionId(request));
//     }

//     private String sessionId(HttpServletRequest request) {
//         if (request.getCookies() != null) {
//             for (var cookie : request.getCookies()) {
//                 if (SESSION_COOKIE.equals(cookie.getName())) {
//                     return cookie.getValue();
//                 }
//             }
//         }
//         // Anonymous session tracking cookie is set by the SPA on first load
//         // (see frontend PortalSessionService); absence just means we can't
//         // correlate this visitor's events, not that the request is rejected.
//         return request.getSession(true).getId();
//     }
// }

package com.ricozknow.portal;

import com.ricozknow.analytics.AnalyticsEventService;
import com.ricozknow.analytics.AnalyticsEventType;
import com.ricozknow.common.RateLimiter;
import com.ricozknow.common.TooManyRequestsException;
import com.ricozknow.portal.dto.PortalArticleDetail;
import com.ricozknow.portal.dto.PortalArticleSummary;
import com.ricozknow.search.SearchService;
import com.ricozknow.search.dto.SearchResultResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

/**
 * Everything here is anonymous-accessible (spec section 2's "Customer Portal").
 * Tenant is resolved from the request hostname by TenantResolverFilter same as
 * any other request — no separate tenant param needed on these endpoints.
 */
@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
public class PortalController {

    private static final String SESSION_COOKIE = "rk_session";
    private static final int SEARCH_PAGE_SIZE = 10;

    private final PortalArticleService portalArticleService;
    private final SearchService searchService;
    private final AnalyticsEventService analyticsEventService;
    private final RateLimiter rateLimiter;

    @Value("${ricozknow.security.rate-limits.search.max-requests}")
    private int searchMaxRequests;
    @Value("${ricozknow.security.rate-limits.search.window-seconds}")
    private long searchWindowSeconds;

    @GetMapping("/articles")
    public Page<PortalArticleSummary> browse(@RequestParam(required = false) UUID categoryId, Pageable pageable) {
        return portalArticleService.browse(categoryId, pageable);
    }

    @GetMapping("/articles/{slug}")
    public PortalArticleDetail get(@PathVariable String slug, HttpServletRequest request) {
        return portalArticleService.getBySlug(slug, sessionId(request));
    }

    /** Real OpenSearch-backed search (Phase 4), replacing the Phase 3 naive DB stub entirely. */
    @GetMapping("/search")
    public SearchResultResponse search(@RequestParam String q,
                                        @RequestParam(required = false) UUID categoryId,
                                        @RequestParam(defaultValue = "0") int page,
                                        HttpServletRequest request) {
        String session = sessionId(request);
        // Anonymous, unauthenticated, and hits OpenSearch on every call — the
        // one portal endpoint most worth throttling per visitor (spec section
        // 22: "Search rate limiting").
        if (!rateLimiter.allow("search:" + session, searchMaxRequests, Duration.ofSeconds(searchWindowSeconds))) {
            throw new TooManyRequestsException("Too many searches. Please slow down.");
        }
        return searchService.search(q, categoryId, page, SEARCH_PAGE_SIZE, session);
    }

    /**
     * Records that a visitor clicked a search result (Phase 8). This is the
     * precise "did they find it" signal spec section 16's SEARCH_RESULT_CLICKED
     * event type always intended — AnalyticsAggregationService now uses this
     * instead of the earlier "viewed something within 10 minutes" proxy to
     * decide whether a search was successful.
     *
     * Fire-and-forget from the frontend: called just before navigating to the
     * clicked article, so it deliberately doesn't block on a response body.
     */
    @PostMapping("/search/click")
    public void recordClick(@RequestParam String query, @RequestParam UUID articleId, HttpServletRequest request) {
        String metadata = "{\"query\":\"" + query.replace("\"", "") + "\"}";
        analyticsEventService.record(AnalyticsEventType.SEARCH_RESULT_CLICKED, sessionId(request), null,
                articleId, null, metadata);
    }

    @PostMapping("/articles/{id}/feedback")
    public void feedback(@PathVariable UUID id, @RequestParam boolean helpful, HttpServletRequest request) {
        portalArticleService.recordFeedback(id, helpful, sessionId(request));
    }

    private String sessionId(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (SESSION_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        // Anonymous session tracking cookie is set by the SPA on first load
        // (see frontend PortalSessionService); absence just means we can't
        // correlate this visitor's events, not that the request is rejected.
        return request.getSession(true).getId();
    }
}